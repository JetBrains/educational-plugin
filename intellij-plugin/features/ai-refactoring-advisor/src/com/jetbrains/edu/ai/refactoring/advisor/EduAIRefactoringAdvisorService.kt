package com.jetbrains.edu.ai.refactoring.advisor

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.jetbrains.edu.ai.clippy.assistant.AIClippyService
import com.jetbrains.edu.ai.clippy.assistant.AIClippyService.ClippyLinkAction
import com.jetbrains.edu.ai.learner.feedback.AILearnerFeedbackService
import com.jetbrains.edu.ai.refactoring.advisor.grazie.AIRefactoringAdvisorGrazieClient
import com.jetbrains.edu.ai.refactoring.advisor.messages.EduAIRefactoringAdvisorBundle
import com.jetbrains.edu.ai.refactoring.advisor.prompts.AIRefactoringSystemContext
import com.jetbrains.edu.ai.refactoring.advisor.prompts.AIRefactoringUserContext
import com.jetbrains.edu.learning.actions.ApplyCodeAction
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.getCodeTaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.getTextFromTaskTextFile
import com.jetbrains.edu.learning.notification.EduNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class EduAIRefactoringAdvisorService(private val project: Project, private val scope: CoroutineScope) {
  fun getClippyComments() {
    scope.launch(Dispatchers.EDT) {
      val course = project.course ?: return@launch
      val task = project.getCurrentTask() ?: return@launch
      val taskFile = task.getCodeTaskFile(project) ?: return@launch
      val taskVirtualFile = taskFile.getVirtualFile(project) ?: error("No virtual file found for task $task")
      val userCode = taskVirtualFile.getTextFromTaskTextFile() ?: error("No text file for taskFile in task $task")
      val taskDescription = task.getDescriptionFile(project)?.getTextFromTaskTextFile() ?: error("No description text")

      val initialCode = taskFile.contents.textualRepresentation
      val userCodeDiff = getInitialToUserCodeDiff(initialCode, userCode)
      val refactoringContext = AIRefactoringUserContext(userCodeDiff, initialCode, taskDescription)
      val refactoringSystemContext = AIRefactoringSystemContext(course.languageId)

      val clippyDiff = runWithModalProgressBlocking(project, "Getting clippy notes") {
        withContext(Dispatchers.IO) {
          AIRefactoringAdvisorGrazieClient.generateRefactoringPatch(refactoringContext, refactoringSystemContext).dropFormatting()
        }
      }

      val clippySuggestedCode = applyPatch(initialCode, clippyDiff)
      val filteredResult = filterSuggestedCode(userCode, clippySuggestedCode)
      if (userCode == filteredResult) {
        EduNotificationManager.showInfoNotification(
          project,
          EduAIRefactoringAdvisorBundle.message("refactoring.diff.no.suggestions.title"),
          EduAIRefactoringAdvisorBundle.message("refactoring.diff.no.suggestions.content")
        )
        return@launch
      }
      showFullDiff(userCode, filteredResult, taskVirtualFile)
    }
  }

  fun showRefactoringLinkInClippy() {
    scope.launch {
      val feedback = AILearnerFeedbackService.getInstance(project).getFeedback(positive = true)
      val clippyLink = ClippyLinkAction(EduAIRefactoringAdvisorBundle.message("refactoring.diff.action.show")) {
        getClippyComments()
      }
      AIClippyService.getInstance(project).showWithTextAndLinks(feedback, listOf(clippyLink))
    }
  }

  private suspend fun showFullDiff(userCode: String, suggestedCode: String, taskVirtualFile: VirtualFile) {
    val diffRequestChain = SimpleDiffRequestChain(
      SimpleDiffRequest(
        EduAIRefactoringAdvisorBundle.message("refactoring.diff.title"),
        DiffContentFactory.getInstance().create(userCode, taskVirtualFile.fileType),
        DiffContentFactory.getInstance().create(suggestedCode, taskVirtualFile.fileType),
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

  /**
   * This is a temporary workaround.
   * It returns the modified or unchanged lines from the generated suggestion, but ignores lines where only comment has been added.
   */
  private fun filterSuggestedCode(userCode: String, suggestedCode: String): String =
    userCode.lines().zip(suggestedCode.lines()).joinToString("\n") {
      when {
        it.first == it.second -> it.first
        it.first.contains("#") && it.second.contains("#") -> it.first
        it.second.startsWith(it.first) && it.second.substringAfter(it.first).trim().startsWith("#") -> it.first
        else -> it.second
      }
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