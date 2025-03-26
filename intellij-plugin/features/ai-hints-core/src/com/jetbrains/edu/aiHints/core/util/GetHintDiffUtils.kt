package com.jetbrains.edu.aiHints.core.util

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.editor.ChainDiffVirtualFile
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.asSafely
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.actions.ApplyCodeAction

internal fun createCodeHintDiff(
  taskFileText: String,
  taskVirtualFile: VirtualFile,
  codeHint: String
): SimpleDiffRequestChain {
  val diffRequestChain = SimpleDiffRequestChain(
    SimpleDiffRequest(
      EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.diff.title"),
      DiffContentFactory.getInstance().create(taskFileText, taskVirtualFile.fileType),
      DiffContentFactory.getInstance().create(codeHint, taskVirtualFile.fileType),
      EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.current.solution"),
      EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.solution.after.changes")
    )
  )
  diffRequestChain.putUserData(ApplyCodeAction.VIRTUAL_FILE_PATH_LIST, listOf(taskVirtualFile.path)) // Needed for ApplyCodeAction
  diffRequestChain.putUserData(ApplyCodeAction.GET_HINT_DIFF, true) // Needed for distinguishing the Diff with the Code Hint from other
  return diffRequestChain
}

internal fun findCodeHintDiffFile(project: Project): VirtualFile? = FileEditorManager.getInstance(project).openFiles.firstOrNull {
  it.asSafely<ChainDiffVirtualFile>()?.chain?.getUserData(ApplyCodeAction.GET_HINT_DIFF) == true
}