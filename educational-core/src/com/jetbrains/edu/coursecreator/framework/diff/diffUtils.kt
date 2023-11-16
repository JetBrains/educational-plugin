package com.jetbrains.edu.coursecreator.framework.diff

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.framework.impl.FLTaskState
import com.jetbrains.edu.learning.isUnitTestMode
import org.jetbrains.annotations.TestOnly

fun applyChangesWithMergeDialog(
  project: Project,
  task: Task,
  leftState: FLTaskState,
  baseState: FLTaskState,
  rightState: FLTaskState,
  currentTaskName: String,
  targetTaskName: String,
  taskDir: VirtualFile,
  initialBaseState: FLTaskState = baseState
): Boolean {
  val mergeProvider = FLMergeProvider(project, task, leftState, baseState, rightState, initialBaseState)
  val mergeDialogCustomizer = FLMergeDialogCustomizer(project, currentTaskName, targetTaskName)
  val files = baseState.keys.mapNotNull { taskDir.findFileByRelativePath(it) }
  return showMultipleFileMergeDialog(project, files, mergeProvider, mergeDialogCustomizer, currentTaskName, targetTaskName)
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
    MOCK ?: error("You should set mock ui via `withMockSelectTaskUi`")
  }
  else {
    FLMultipleFileMergeUIImpl()
  }
  return ui.show(project, files, mergeProvider, mergeCustomizer, currentTaskName, targetTaskName)
}