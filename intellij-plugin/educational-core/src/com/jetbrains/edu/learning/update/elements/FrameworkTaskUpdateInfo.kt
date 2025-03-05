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
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.toCourseInfoHolder
import com.jetbrains.edu.learning.update.FrameworkLessonTaskHistory
import com.jetbrains.edu.learning.update.UpdateUtils.updateTaskDescription
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

data class FrameworkTaskUpdateInfo(
  override val localItem: Task,
  override val remoteItem: Task,
  private val taskHistory: FrameworkLessonTaskHistory,
  private val isTemplateBased: Boolean
) : TaskUpdate(localItem, remoteItem) {

  override suspend fun update(project: Project) {
    val flManager = FrameworkLessonManager.getInstance(project)

    val localLesson = localItem.parent as FrameworkLesson

    val taskIsCurrent = localLesson.currentTaskIndex == localItem.index - 1

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
    if (taskIsCurrent) {
      val taskDir = localItem.getDir(project.courseDir) ?: return

      for ((fileName, fileHistory) in taskHistory.taskFileHistories) {
        val fileContents = fileHistory.evaluateContents(localLesson.currentTaskIndex, isTemplateBased)
        if (fileContents == null) {
          removeFile(taskDir, fileName)
        }
        else {
          val isEditable = remoteItem.taskFiles[fileName]?.isEditable != false
          updateFile(project, taskDir, fileName, fileContents, isEditable)
        }
      }
    }

    flManager.updateUserChanges(localItem, remoteItem.taskFiles.mapValues { it.value.text })
    localLesson.replaceItem(localItem, remoteItem)
    remoteItem.init(localLesson, false)

    blockingContext {
      YamlFormatSynchronizer.saveItemWithRemoteInfo(remoteItem)
    }
  }

  private suspend fun removeFile(taskDir: VirtualFile, fileName: String) = writeAction {
    val virtualChangedFile = taskDir.findFileByRelativePath(fileName)
    virtualChangedFile?.delete(this)
  }

  private suspend fun updateFile(project: Project, taskDir: VirtualFile, fileName: String, contents: FileContents, isEditable: Boolean) {
    // First delete the file if it exists, then write it

    writeAction {
      val virtualChangedFile = taskDir.findFileByRelativePath(fileName)

      if (virtualChangedFile != null) {
        ReadOnlyAttributeUtil.setReadOnlyAttribute(virtualChangedFile, false)
        virtualChangedFile.delete(this)
      }
    }

    blockingContext {
      GeneratorUtils.createChildFile(project.toCourseInfoHolder(), taskDir, fileName, contents, isEditable)
    }
  }
}