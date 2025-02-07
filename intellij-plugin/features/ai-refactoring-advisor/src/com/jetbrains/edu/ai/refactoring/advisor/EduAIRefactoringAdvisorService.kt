package com.jetbrains.edu.ai.refactoring.advisor

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.comparison.ComparisonManager
import com.intellij.diff.comparison.ComparisonPolicy
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.edu.ai.clippy.assistant.AIClippyService
import com.jetbrains.edu.ai.clippy.assistant.AIClippyService.ClippyLinkAction
import com.jetbrains.edu.ai.learner.feedback.AILearnerFeedbackService
import com.jetbrains.edu.ai.refactoring.advisor.grazie.AIRefactoringAdvisorGrazieClient
import com.jetbrains.edu.ai.refactoring.advisor.messages.EduAIRefactoringAdvisorBundle
import com.jetbrains.edu.ai.refactoring.advisor.prompts.AIRefactoringUserContext
import com.jetbrains.edu.ai.refactoring.advisor.prompts.AIRefactoringSystemContext
import com.jetbrains.edu.ai.refactoring.advisor.ui.EduAIRefactoringAdvisorColors
import com.jetbrains.edu.learning.actions.ApplyCodeAction
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.getCodeTaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.getTextFromTaskTextFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Font

@Service(Service.Level.PROJECT)
class EduAIRefactoringAdvisorService(private val project: Project, private val scope: CoroutineScope) {
  suspend fun getClippyComments() {
    val course = project.course ?: return
    val task = project.getCurrentTask() ?: return
    val taskFile = task.getCodeTaskFile(project) ?: return
    val taskVirtualFile = taskFile.getVirtualFile(project) ?: error("No virtual file found for task $task")
    val userCode = taskVirtualFile.getTextFromTaskTextFile() ?: error("No text file for taskFile in task $task")
    val taskDescription = task.getDescriptionFile(project)?.getTextFromTaskTextFile() ?: error("No description text")

    val initialCode = taskFile.contents.textualRepresentation
    val userCodeDiff = getInitialToUserCodeDiff(initialCode, userCode)
    val refactoringContext = AIRefactoringUserContext(userCodeDiff, initialCode, taskDescription)
    val refactoringSystemContext = AIRefactoringSystemContext(course.languageId)

    val clippyDiff = withBackgroundProgress(project, "Getting clippy notes", cancellable = true) {
      withContext(Dispatchers.IO) {
        AIRefactoringAdvisorGrazieClient.generateRefactoringPatch(refactoringContext, refactoringSystemContext).dropFormatting()
      }
    }
    val clippySuggestedCode = applyPatch(initialCode, clippyDiff)
    withContext(Dispatchers.EDT) {
      highlightFirstCodeDiffPositionOrNull(project, taskVirtualFile, clippySuggestedCode, userCode)
      showFullDiff(userCode, clippySuggestedCode, taskVirtualFile)
    }
  }

  fun showRefactoringLinkInClippy() {
    scope.launch {
      val feedback = AILearnerFeedbackService.getInstance(project).getFeedback(positive = true)
      val clippyLink = ClippyLinkAction(EduAIRefactoringAdvisorBundle.message("refactoring.diff.action.show")) {
        currentThreadCoroutineScope().launch { getClippyComments() }
      }
      AIClippyService.getInstance(project).showWithTextAndLinks(feedback, listOf(clippyLink))
    }
  }

  private suspend fun showFullDiff(userCode: String, suggestedCode: String, taskVirtualFile: VirtualFile) {
    val diffRequestChain = SimpleDiffRequestChain(
      SimpleDiffRequest(
        EduAIRefactoringAdvisorBundle.message("refactoring.diff.title"),
        DiffContentFactory.getInstance().create(userCode, FileTypes.PLAIN_TEXT),
        DiffContentFactory.getInstance().create(suggestedCode, FileTypes.PLAIN_TEXT),
        EduAIRefactoringAdvisorBundle.message("refactoring.diff.before"),
        EduAIRefactoringAdvisorBundle.message("refactoring.diff.after")
      )
    )
    diffRequestChain.putUserData(ApplyCodeAction.VIRTUAL_FILE_PATH_LIST, listOf(taskVirtualFile.path))
    diffRequestChain.putUserData(GET_AI_REFACTORING_DIFF, true)
    withContext(Dispatchers.EDT) {
      DiffManager.getInstance().showDiff(project, diffRequestChain, DiffDialogHints.FRAME)
    }
  }

  @RequiresEdt
  private fun highlightFirstCodeDiffPositionOrNull(
    project: Project,
    taskVirtualFile: VirtualFile,
    taskFileText: String,
    newText: String
  ): RangeHighlighter? {
    val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
    val currentVirtualFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return null
    if (currentVirtualFile != taskVirtualFile) return null

    val startLine = ComparisonManager.getInstance().compareLines(
      taskFileText, newText, ComparisonPolicy.DEFAULT, DumbProgressIndicator.INSTANCE
    ).firstOrNull()?.startLine1 ?: return null
    if (startLine >= taskFileText.lines().size) return null

    val attributes = TextAttributes(null, EduAIRefactoringAdvisorColors.wellDoneHighlight, null, EffectType.BOXED, Font.PLAIN)
    return editor.markupModel.addLineHighlighter(startLine, 0, attributes)
  }

  private fun getInitialToUserCodeDiff(initialCode: String, userCode: String): String {
    val initialCodeSplit = initialCode.split("\n")
    val userCodeSplit = userCode.split("\n")
    val diff = DiffUtils.diff(initialCodeSplit, userCodeSplit)
    val diffList = UnifiedDiffUtils.generateUnifiedDiff("", "", initialCodeSplit, diff, 0)
    return diffList.joinToString("\n")
  }

  private fun applyPatch(text: String, patch: String): String {
    val patchObj = UnifiedDiffUtils.parseUnifiedDiff(patch.split("\n"))
    return DiffUtils.patch(text.split("\n"), patchObj).joinToString("\n")
  }

  private fun String.dropFormatting() = split("\n").drop(1).dropLast(1).joinToString("\n")

  companion object {
    private val GET_AI_REFACTORING_DIFF = Key.create<Boolean>("getAIRefactoringDiff")

    fun getInstance(project: Project): EduAIRefactoringAdvisorService = project.service()
  }
}