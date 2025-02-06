package com.jetbrains.edu.ai.clippy.assistant

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
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.edu.ai.clippy.assistant.grazie.ClippySuggestionsExtractor
import com.jetbrains.edu.ai.clippy.assistant.messages.EduAIClippyAssistantBundle
import com.jetbrains.edu.learning.actions.ApplyCodeAction
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.getCodeTaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.getTextFromTaskTextFile
import com.jetbrains.edu.learning.ui.EduColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Font

@Service(Service.Level.PROJECT)
class ClippyDiffService(private val project: Project) {
  suspend fun getClippyComments() {
    val course = project.course ?: return
    val task = project.getCurrentTask() ?: return
    val taskFile = task.getCodeTaskFile(project) ?: return
    val taskVirtualFile = taskFile.getVirtualFile(project) ?: error("No virtual file found for task $task")
    val userCode = taskVirtualFile.getTextFromTaskTextFile() ?: error("No text file for taskFile in task $task")
    val taskDescription = task.getDescriptionFile(project)?.getTextFromTaskTextFile() ?: error("No description text")

    val initialCode = taskFile.contents.textualRepresentation
    val userCodeDiff = getInitialToUserCodeDiff(initialCode, userCode)

    val clippyDiff = withBackgroundProgress(project, "Getting clippy notes", cancellable = true) {
      withContext(Dispatchers.IO) {
        ClippySuggestionsExtractor.getClippyPatch(course.languageId, userCodeDiff, initialCode, taskDescription)
      }
    }
    val clippySuggestedCode = applyPatch(userCode, clippyDiff)
    withContext(Dispatchers.EDT) {
      highlightFirstCodeDiffPositionOrNull(project, taskVirtualFile, clippySuggestedCode, userCode)
      showFullDiff(userCode, clippySuggestedCode, taskVirtualFile)
    }
  }

  private suspend fun showFullDiff(userCode: String, suggestedCode: String, taskVirtualFile: VirtualFile) {
    val diffRequestChain = SimpleDiffRequestChain(
      SimpleDiffRequest(
        EduAIClippyAssistantBundle.message("clippy.diff.title"),
        DiffContentFactory.getInstance().create(userCode, FileTypes.PLAIN_TEXT),
        DiffContentFactory.getInstance().create(suggestedCode, FileTypes.PLAIN_TEXT),
        EduAIClippyAssistantBundle.message("clippy.diff.before"),
        EduAIClippyAssistantBundle.message("clippy.diff.after")
      )
    )
    diffRequestChain.putUserData(ApplyCodeAction.VIRTUAL_FILE_PATH_LIST, listOf(taskVirtualFile.path))
    diffRequestChain.putUserData(GET_CLIPPY_DIFF, true)
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

    val attributes = TextAttributes(null, EduColors.aiGetClippyWellDoneHighlight, null, EffectType.BOXED, Font.PLAIN)
    return editor.markupModel.addLineHighlighter(startLine, 0, attributes)
  }

  private fun getInitialToUserCodeDiff(initialCode: String, userCode: String): String {
    val initialCodeSplit = initialCode.split("\n")
    val userCodeSplit = userCode.split("\n")
    val diff = DiffUtils.diff(initialCodeSplit, userCodeSplit)
    val diffList = UnifiedDiffUtils.generateUnifiedDiff("", "", initialCodeSplit, diff, 0)
    return diffList.drop(2).joinToString("\n")
  }

  private fun applyPatch(text: String, patch: String): String {
    val patchObj = UnifiedDiffUtils.parseUnifiedDiff(patch.split("\n"))
    return DiffUtils.patch(text.split("\n"), patchObj).joinToString("\n")
  }

  companion object {
    private val GET_CLIPPY_DIFF = Key.create<Boolean>("getClippyDiff")

    fun getInstance(project: Project): ClippyDiffService = project.service()
  }
}