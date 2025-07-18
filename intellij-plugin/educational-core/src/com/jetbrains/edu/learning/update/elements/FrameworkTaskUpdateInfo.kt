package com.jetbrains.edu.learning.update.elements

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.ReadOnlyAttributeUtil
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.FileContents
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.getTaskDirectory
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.toCourseInfoHolder
import com.jetbrains.edu.learning.update.FrameworkLessonHistory
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import java.io.IOException

data class FrameworkTaskUpdateInfo(
  override val localItem: Task,
  override val remoteItem: Task,
  private val taskHistory: FrameworkLessonHistory
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

    try {
      updateTaskDirectory(project, localLesson)
    }
    catch (e: Exception) {
      thisLogger().error("Failed to update task directory", e)
      return
    }

    remoteItem.record = localItem.record
    flManager.updateUserChanges(localItem, remoteItem.taskFiles.mapValues { it.value.text })
    localLesson.replaceItem(localItem, remoteItem)
    remoteItem.init(localLesson, false)

    if (taskIsCurrent) {
      val taskDir = remoteItem.getDir(project.courseDir) ?: return

      for ((fileName, fileHistory) in taskHistory.taskFileHistories) {
        val fileContents = fileHistory.evaluateContents(localLesson.currentTaskIndex)
        if (fileContents == null) {
          removeFile(taskDir, fileName)
        }
        else {
          val isEditable = remoteItem.taskFiles[fileName]?.isEditable != false
          updateFile(project, taskDir, fileName, fileContents, isEditable)
        }
      }
    }

    YamlFormatSynchronizer.saveItemWithRemoteInfo(remoteItem)
  }

  @Throws(IOException::class, IllegalStateException::class)
  private suspend fun updateTaskDirectory(project: Project, localLesson: FrameworkLesson) {
    val newTaskDir = if (localItem.name != remoteItem.name) {
      createTaskDirectoryWithNewName(project, localLesson)
    }
    else {
      localItem.getTaskDirectory(project) ?: error("Failed to find local task dir: ${localLesson.name}")
    }

    GeneratorUtils.createDescriptionFile(project, newTaskDir, remoteItem)
  }

  @Throws(IOException::class, IllegalStateException::class)
  private suspend fun createTaskDirectoryWithNewName(project: Project, localLesson: FrameworkLesson): VirtualFile {
    val localTaskDir = localItem.getTaskDirectory(project) ?: error("Failed to find local task dir on update: ${localLesson.name}")

    writeAction {
      localTaskDir.delete(FrameworkTaskUpdateInfo::class)
    }

    val lessonDir = localLesson.getDir(project.courseDir) ?: error("Failed to find local lesson dir on update: ${localLesson.name}")

    val newTaskDir = writeAction {
      VfsUtil.createDirectoryIfMissing(lessonDir, remoteItem.name)
    }

    return newTaskDir
  }

  private suspend fun removeFile(taskDir: VirtualFile, fileName: String) = writeAction {
    val virtualChangedFile = taskDir.findFileByRelativePath(fileName)
    virtualChangedFile?.delete(this)
  }

  private suspend fun updateFile(project: Project, taskDir: VirtualFile, fileName: String, contents: FileContents, isEditable: Boolean) {

    val virtualChangedFile = readAction {
      taskDir.findFileByRelativePath(fileName)
    }

    if (virtualChangedFile != null) {
      writeAction {
        FileDocumentManager.getInstance().reloadFiles(virtualChangedFile)
        ReadOnlyAttributeUtil.setReadOnlyAttribute(virtualChangedFile, false)
      }
    }

    GeneratorUtils.createChildFile(project.toCourseInfoHolder(), taskDir, fileName, contents, isEditable)
  }
}