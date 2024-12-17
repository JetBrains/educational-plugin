package com.jetbrains.edu.aiHints.core.ui

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.comparison.ComparisonManager
import com.intellij.diff.comparison.ComparisonPolicy
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.actions.ApplyCodeAction
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.getTextFromTaskTextFile
import com.jetbrains.edu.learning.ui.EduColors
import com.jetbrains.educational.ml.hints.hint.CodeHint
import org.jetbrains.annotations.Nls
import java.awt.Font

class CodeHintInlineBanner(
  private val project: Project,
  taskFile: TaskFile,
  message: @Nls String,
  private val codeHint: CodeHint
) : HintInlineBanner(project, message) {

  private val highlighter: RangeHighlighter?

  init {
    val taskVirtualFile = taskFile.getVirtualFile(project) ?: error("VirtualFile for ${taskFile.name} not found")
    val taskFileText = taskVirtualFile.getTextFromTaskTextFile() ?: error("TaskFile text for ${taskFile.name} not found")
    highlighter = highlightFirstCodeDiffPositionOrNull(project, taskVirtualFile, taskFileText, codeHint.code)

    addAction(EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.show.code.text")) {
      showInCodeAction(project, taskVirtualFile, taskFileText, codeHint.code)
    }
  }

  override fun removeNotify() {
    super.removeNotify()
    highlighter?.dispose()
  }


  private fun showInCodeAction(project: Project, taskVirtualFile: VirtualFile, taskFileText: String, codeHint: String) {
    val diffRequestChain = SimpleDiffRequestChain(
      SimpleDiffRequest(
        EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.diff.title"),
        DiffContentFactory.getInstance().create(taskFileText, taskVirtualFile.fileType),
        DiffContentFactory.getInstance().create(codeHint, taskVirtualFile.fileType),
        EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.current.solution"),
        EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.solution.after.changes")
      )
    )
    diffRequestChain.putUserData(ApplyCodeAction.VIRTUAL_FILE_PATH_LIST, listOf(taskVirtualFile.path))
    diffRequestChain.putUserData(ApplyCodeAction.GET_HINT_DIFF, true)
    DiffManager.getInstance().showDiff(project, diffRequestChain, DiffDialogHints.FRAME)
  }

  private fun highlightFirstCodeDiffPositionOrNull(
    project: Project,
    taskVirtualFile: VirtualFile,
    taskFileText: String,
    codeHint: String
  ): RangeHighlighter? {
    val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
    val currentVirtualFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return null
    if (currentVirtualFile != taskVirtualFile) return null

    val startLine = ComparisonManager.getInstance().compareLines(
      taskFileText, codeHint, ComparisonPolicy.DEFAULT, DumbProgressIndicator.INSTANCE
    ).firstOrNull()?.startLine1 ?: return null
    if (startLine >= taskFileText.lines().size) return null

    val attributes = TextAttributes(null, EduColors.aiGetHintHighlighterColor, null, EffectType.BOXED, Font.PLAIN)
    return editor.markupModel.addLineHighlighter(startLine, 0, attributes)
  }
}