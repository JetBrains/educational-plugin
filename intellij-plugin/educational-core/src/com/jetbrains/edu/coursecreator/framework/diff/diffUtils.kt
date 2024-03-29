package com.jetbrains.edu.coursecreator.framework.diff

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.PathUtil
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.framework.impl.FLTaskState
import com.jetbrains.edu.learning.isUnitTestMode
import org.jetbrains.annotations.TestOnly
import java.nio.file.Path
import java.nio.file.Paths

class FLLightVirtualFile(
  private val filePath: String,
  fileType: FileType,
  content: String,
) : LightVirtualFile(PathUtil.getFileName(filePath), fileType, content) {
  override fun toNioPath(): Path {
    return Paths.get(filePath)
  }

  override fun getPath(): String {
    return filePath
  }

  override fun delete(requestor: Any?) {
    isValid = false
  }
}

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
  val mergeProvider = FLMergeProvider(leftState, baseState, rightState, initialBaseState)
  val mergeDialogCustomizer = FLMergeDialogCustomizer(currentTask.name, targetTask.name)
  val conflictLightVirtualFiles = conflictFiles.map { path ->
    val fileType = findConflictFileType(project, path, currentTask, targetTask) ?: error("Couldn't find file corresponding for $path")
    val fileContent = baseState[path] ?: error("Conflict file content was not added to baseState during conflict resolution")
    // set file name with a full file path
    FLLightVirtualFile(path, fileType, fileContent)
  }
  val isOk = showMultipleFileMergeDialog(project, conflictLightVirtualFiles, mergeProvider, mergeDialogCustomizer, currentTask.name, targetTask.name)

  // merge dialog was canceled
  if (!isOk) {
    return null
  }

  val finalState = baseState.toMutableMap()

  for (file in conflictLightVirtualFiles) {
    // file was deleted
    if (!file.exists()) {
      finalState.remove(file.path)
      continue
    }

    val document = runReadAction { FileDocumentManager.getInstance().getDocument(file) } ?: error("There is no document for ${file.path}")

    finalState[file.path] = document.text
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

private fun showMultipleFileMergeDialog(
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

private fun findConflictFileType(project: Project, fileName: String, currentTask: Task, targetTask: Task): FileType? {
  // conflict file must exist as virtual file on disk either in the current task or target task
  // otherwise there cannot be a conflict
  return findFileType(project, fileName, currentTask) ?: findFileType(project, fileName, targetTask)
}

private fun findFileType(project: Project, fileName: String, task: Task): FileType? {
  val taskFile = task.taskFiles[fileName] ?: return null
  return taskFile.getVirtualFile(project)?.fileType
}