package com.jetbrains.edu.learning.update.elements

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.progress.blockingContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.ReadOnlyAttributeUtil
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.FileContents
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.InMemoryUndeterminedContents
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.shouldBePropagated
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.framework.impl.FLTaskState
import com.jetbrains.edu.learning.toCourseInfoHolder
import com.jetbrains.edu.learning.update.UpdateUtils.updateTaskDescription
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

data class FrameworkTaskUpdateInfo(override val localItem: Task, override val remoteItem: Task) : TaskUpdate(localItem, remoteItem) {
  override suspend fun update(project: Project) {
    val flManager = FrameworkLessonManager.getInstance(project)

    val lesson = localItem.parent as FrameworkLesson
    val taskIsCurrent = lesson.currentTaskIndex == localItem.index - 1

    if (localItem.status != CheckStatus.Solved) {
      remoteItem.status = CheckStatus.Unchecked
    }
    else {
      remoteItem.status = CheckStatus.Solved
    }

    blockingContext {
      updateTaskDescription(project, localItem, remoteItem)
    }

    remoteItem.record = localItem.record
    val updatedInitialState = flManager.getTaskInitialState(remoteItem)

    if (taskIsCurrent) {
      val taskState = flManager.getTaskState(lesson, localItem)
      val taskIsModified = taskState != flManager.getTaskInitialState(localItem)

      val previousTask = lesson.taskList.getOrNull(lesson.currentTaskIndex - 1)
      val isTemplateBased = lesson.isTemplateBased && lesson.course !is HyperskillCourse

      val newStateIfTaskIsUnmodified = if (previousTask == null || isTemplateBased) {
        updatedInitialState
      }
      else {
        flManager.getTaskState(lesson, previousTask)
      }

      CurrentTaskUpdater(project, localItem, remoteItem, taskIsModified, newStateIfTaskIsUnmodified, lesson.isTemplateBased).updateInLocalFS()
    }

    flManager.updateUserChanges(localItem, updatedInitialState)
    lesson.replaceItem(localItem, remoteItem)
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
  private val taskIsModified: Boolean,
  private val newStateIfTaskIsUnmodified: FLTaskState,
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

      if (localFile == null && remoteFile == null) continue // impossible, but needed for the Kotlin type inference.

      val localIsPropagatable = !isTemplateBased && localFile?.shouldBePropagated() == true
      val remoteIsPropagatable = !isTemplateBased && remoteFile?.shouldBePropagated() == true

      when {
        localFile == null && remoteIsPropagatable -> propagatableFileCreated(remoteFile)
        localFile == null -> nonPropagatableFileCreated(remoteFile!!) // Seems to be a Kotlin issue; it must infer that remoteFile != null
        remoteFile == null && localIsPropagatable -> propagatableFileRemoved(localFile)
        remoteFile == null -> nonPropagatableFileRemoved(localFile)
        remoteIsPropagatable -> propagatableFileChanged(remoteFile)
        !remoteIsPropagatable -> nonPropagatableFileChanged(remoteFile)
      }
    }
  }

  private suspend fun propagatableFileChanged(taskFile: TaskFile) {
    if (taskIsModified) return

    val updatedText = newStateIfTaskIsUnmodified[taskFile.name] ?: return
    updateFile(taskFile, textToFileContents(updatedText))
  }

  private suspend fun nonPropagatableFileChanged(taskFile: TaskFile) = updateFile(taskFile)

  private suspend fun propagatableFileCreated(taskFile: TaskFile) {
    // we don't check here whether the task is modified, because we don't spoil user changes by creating a new file
    val textToUpdate = newStateIfTaskIsUnmodified[taskFile.name] ?: return
    updateFile(taskFile, textToFileContents(textToUpdate))
  }

  private suspend fun nonPropagatableFileCreated(taskFile: TaskFile) = updateFile(taskFile)

  private suspend fun propagatableFileRemoved(taskFile: TaskFile) {
    if (!taskIsModified) removeFile(taskFile)
  }

  private suspend fun nonPropagatableFileRemoved(taskFile: TaskFile) = removeFile(taskFile)

  private suspend fun removeFile(taskFile: TaskFile) = writeAction {
    val virtualChangedFile = taskFile.getVirtualFile(project)
    virtualChangedFile?.delete(this)
  }

  private suspend fun updateFile(taskFile: TaskFile, contents: FileContents = taskFile.contents) {
    // First delete the file if it exists, then write it

    writeAction {
      val virtualChangedFile = taskFile.getVirtualFile(project)

      if (virtualChangedFile != null) {
        ReadOnlyAttributeUtil.setReadOnlyAttribute(virtualChangedFile, false)
        virtualChangedFile.delete(this)
      }
    }

    blockingContext {
      GeneratorUtils.createChildFile(project.toCourseInfoHolder(), taskDir, taskFile.name, contents, taskFile.isEditable)
    }
  }

  /**
   * Currently, the state of the task is stored with String values independently of whether a file is binary or not.
   * So we convert the text to [FileContents] in such a way, that the [GeneratorUtils.createChildFile] method should
   * try to determine file binarity.
   * This method will not be needed after task states are stored respecting file binarity.
   */
  private fun textToFileContents(text: String): FileContents = InMemoryUndeterminedContents(text)
}
