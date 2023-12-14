package com.jetbrains.edu.coursecreator.framework.diff

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.PathUtil
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.framework.impl.FLTaskState
import com.jetbrains.edu.learning.isUnitTestMode
import org.jetbrains.annotations.TestOnly

fun applyChangesWithMergeDialog(
  project: Project,
  currentTask: Task,
  targetTask: Task,
  conflictFiles: List<String>,
  leftState: FLTaskState,
  baseState: FLTaskState,
  rightState: FLTaskState,
  initialBaseState: FLTaskState = baseState
): FLTaskState? {
  val mergeProvider = FLMergeProvider(project, targetTask, leftState, baseState, rightState, initialBaseState)
  val mergeDialogCustomizer = FLMergeDialogCustomizer(project, currentTask.name, targetTask.name)
  val tmpConflictFiles = conflictFiles.map {
    val fileType = FileTypeManager.getInstance().getFileTypeByExtension(PathUtil.getFileExtension(it) ?: "")
    LightVirtualFile(it, fileType, baseState[it] ?: "")
  }
  val isOk = showMultipleFileMergeDialog(project, tmpConflictFiles, mergeProvider, mergeDialogCustomizer, currentTask.name, targetTask.name)

  // merge dialog was canceled
  if (!isOk) {
    return null
  }

  val finalState = baseState.toMutableMap()

  for (file in tmpConflictFiles) {
    // file was deleted
    if (!file.exists()) {
      finalState.remove(file.name)
    }

    val document = FileDocumentManager.getInstance().getDocument(file)

    finalState[file.name] = document?.text ?: error("There is no document for ${file.name}")
  }

  return finalState
}

private var MOCK: FLMultipleFileMergeUI? = null

@TestOnly
fun withFLMultipleFileMergeUI(mockUi: FLMultipleFileMergeUI, action: () -> Unit) {
  try {
    MOCK = mockUi
    action()
  }
  finally {
    MOCK = null
  }
}

fun showMultipleFileMergeDialog(
  project: Project,
  files: List<VirtualFile>,
  mergeProvider: FLMergeProvider,
  mergeCustomizer: FLMergeDialogCustomizer,
  currentTaskName: String,
  targetTaskName: String
): Boolean {
  val ui = if (isUnitTestMode) {
    MOCK ?: error("You should set mock UI via `withMockSelectTaskUi`")
  }
  else {
    FLMultipleFileMergeUIImpl()
  }
  return ui.show(project, files, mergeProvider, mergeCustomizer, currentTaskName, targetTaskName)
}

fun resolveConflicts(
  project: Project,
  currentState: FLTaskState,
  baseState: FLTaskState,
  targetState: FLTaskState
): FLConflictResolveStrategy.StateWithResolvedChanges {
  return with(DiffConflictResolveStrategy(project)) {
    try {
      resolveConflicts(currentState, baseState, targetState)
    } finally {
      Disposer.dispose(this)
    }
  }
}