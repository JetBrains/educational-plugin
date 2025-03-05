package com.jetbrains.edu.learning.update.elements

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.progress.blockingContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.ReadOnlyAttributeUtil
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.framework.impl.FLTaskState
import com.jetbrains.edu.learning.toCourseInfoHolder
import com.jetbrains.edu.learning.update.UpdateUtils.updateTaskDescription
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

data class FrameworkTaskUpdateInfo(override val localItem: Task, override val remoteItem: Task) : TaskUpdate(localItem, remoteItem) {
  override suspend fun update(project: Project) {
    val lesson = localItem.parent as FrameworkLesson
    val taskIsCurrent = lesson.currentTaskIndex == localItem.index - 1
    val firstTask = lesson.taskList.getOrNull(0)

    if (localItem.status != CheckStatus.Solved) {
      remoteItem.status = CheckStatus.Unchecked
    }
    else {
      remoteItem.status = CheckStatus.Solved
    }

    blockingContext {
      updateTaskDescription(project, localItem, remoteItem)
    }

    if (taskIsCurrent) {
      val taskState = FrameworkLessonManager.getInstance(project).getTaskState(lesson, localItem)
      CurrentTaskUpdater(project, localItem, remoteItem, taskState, firstTask, lesson.isTemplateBased).updateInLocalFS()
    }

    lesson.removeItem(localItem)
    lesson.addItem(remoteItem)
    remoteItem.init(lesson, false)

    blockingContext {
      YamlFormatSynchronizer.saveItemWithRemoteInfo(remoteItem)
    }
  }
}

private class CurrentTaskUpdater(
  private val project: Project,
  private val localTask: Task,
  private val remoteTask: Task,
  private val taskState: FLTaskState,
  private val firstTask: Task?,
  private val isTemplateBased: Boolean
) {

  private lateinit var taskDir: VirtualFile

  suspend fun updateInLocalFS() {
    taskDir = localTask.getDir(project.courseDir) ?: return

    val localFiles = localTask.taskFiles
    val remoteFiles = remoteTask.taskFiles

    val filePaths = localTask.taskFiles.keys.toSet() + remoteTask.taskFiles.keys.toSet()

    for (path in filePaths) {
      val localFile = localFiles[path]
      val remoteFile = remoteFiles[path]

      if (localFile == null && remoteFile == null) continue

      val localIsPropagatable = !isTemplateBased && localFile?.isPropagatable == true
      val remoteIsPropagatable = !isTemplateBased && remoteFile?.isPropagatable == true

      when {
        localFile == null && remoteIsPropagatable -> propagatableFileCreated(remoteFile)
        localFile == null -> nonPropagatableFileCreated(remoteFile!!) // Seems to be a Kotlin issue
        remoteFile == null && localIsPropagatable -> propagatableFileRemoved(localFile)
        remoteFile == null -> nonPropagatableFileRemoved(localFile)
        remoteIsPropagatable -> propagatableFileChanged(remoteFile)
        !remoteIsPropagatable -> nonPropagatableFileChanged(remoteFile)
      }
    }
  }

  private suspend fun propagatableFileChanged(taskFile: TaskFile) {
    val fileState = taskFile.state

    // We don't touch the file if it has user changes
    if (fileState != null) return

    val fileFromTheFirstTask = firstTask?.taskFiles[taskFile.name] ?: return
    updateFile(fileFromTheFirstTask)
  }

  private suspend fun nonPropagatableFileChanged(taskFile: TaskFile) = updateFile(taskFile)

  private suspend fun propagatableFileCreated(taskFile: TaskFile) {
    // it is very un-probable that a user already has a file with the same name, but we are not going to touch it in this case
    val fileState = taskFile.state
    if (fileState != null) return

    val fileFromTheFirstTask = firstTask?.taskFiles[taskFile.name] ?: return
    updateFile(fileFromTheFirstTask)
  }

  private suspend fun nonPropagatableFileCreated(taskFile: TaskFile) = updateFile(taskFile)

  private suspend fun propagatableFileRemoved(taskFile: TaskFile) {
    val fileState = taskFile.state
    if (fileState != null) return
    removeFile(taskFile)
  }

  private suspend fun nonPropagatableFileRemoved(taskFile: TaskFile) = removeFile(taskFile)

  private suspend fun removeFile(taskFile: TaskFile) = writeAction {
    val virtualChangedFile = taskFile.getVirtualFile(project)
    virtualChangedFile?.delete(this)
  }

  private suspend fun updateFile(taskFile: TaskFile) {
    // First delete the file if it exists, then write it

    writeAction {
      val virtualChangedFile = taskFile.getVirtualFile(project)

      if (virtualChangedFile != null) {
        ReadOnlyAttributeUtil.setReadOnlyAttribute(virtualChangedFile, false)
        virtualChangedFile.delete(this)
      }
    }

    blockingContext {
      GeneratorUtils.createChildFile(project.toCourseInfoHolder(), taskDir, taskFile.name, taskFile.contents, taskFile.isEditable)
    }
  }

  private val TaskFile.state
    get() = taskState[name]
}
