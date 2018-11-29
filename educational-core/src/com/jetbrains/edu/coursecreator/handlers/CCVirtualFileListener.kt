package com.jetbrains.edu.coursecreator.handlers

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.*
import com.intellij.util.Function
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.configuration.YamlFormatSynchronizer
import com.jetbrains.edu.coursecreator.stepik.StepikCourseChangeHandler
import com.jetbrains.edu.learning.FileInfo
import com.jetbrains.edu.learning.PlaceholderPainter
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.fileInfo
import com.jetbrains.edu.learning.handlers.EduVirtualFileListener

class CCVirtualFileListener(project: Project) : EduVirtualFileListener(project) {

  override fun beforeFileMovement(event: VirtualFileMoveEvent) {
    val (task, oldPath) = event.file.fileInfo(project) as? FileInfo.FileInTask ?: return
    val (oldParentTask, oldParentPath) = event.oldParent.directoryFileInfo(project) ?: return
    val (newParentTask, newParentPath) = event.newParent.directoryFileInfo(project) ?: return

    if (oldParentTask != newParentTask) {
      LOG.warn("Unsupported case. `EduMoveDelegate` should forbid file moving between different tasks")
      return
    }

    val affectedFiles = mutableListOf<Pair<String, TaskFile>>()
    val oldPaths = task.taskFiles.keys.filter { it.startsWith(oldPath) }

    for (path in oldPaths) {
      val fileObject = task.taskFiles.remove(path) ?: continue
      PlaceholderPainter.hidePlaceholders(fileObject)
      affectedFiles += path to fileObject
    }

    for ((oldFilePath, taskFile) in affectedFiles) {
      var newPath = oldFilePath.removePrefix("$oldParentPath/")
      if (newParentPath.isNotEmpty()) {
        newPath = "$newParentPath/$newPath"
      }

      taskFile.name = newPath
      task.addTaskFile(taskFile)
    }

    StepikCourseChangeHandler.changed(task)
    YamlFormatSynchronizer.saveItem(task)
  }

  /**
   * Handles move events for non course files like drag & drop action produces
   */
  override fun fileMoved(event: VirtualFileMoveEvent) {
    val movedFile = event.file
    val fileInfo = movedFile.fileInfo(project) as? FileInfo.FileInTask ?: return
    val directoryInfo = event.oldParent.directoryFileInfo(project)
    // not null directoryInfo means that we've already processed this file in `beforeFileMovement`
    if (directoryInfo != null) return

    if (movedFile.isDirectory) {
      // We need collect all children files manually
      // because the platform produces move event only for root file
      VfsUtil.visitChildrenRecursively(movedFile, object : VirtualFileVisitor<Any>(VirtualFileVisitor.NO_FOLLOW_SYMLINKS) {
        override fun visitFile(file: VirtualFile): Boolean {
          if (!file.isDirectory) {
            val relativePath = VfsUtil.findRelativePath(movedFile, file, VfsUtilCore.VFS_SEPARATOR_CHAR)
            val newFileInfo = fileInfo.copy(pathInTask = "${fileInfo.pathInTask}${VfsUtilCore.VFS_SEPARATOR_CHAR}$relativePath")
            fileInTaskCreated(newFileInfo)
          }
          return true
        }
      })
    } else {
      fileInTaskCreated(fileInfo)
    }
  }

  private fun VirtualFile.directoryFileInfo(project: Project): FileInfo.FileInTask? {
    val info = fileInfo(project) ?: return null
    return when (info) {
      is FileInfo.TaskDirectory -> FileInfo.FileInTask(info.task, "")
      is FileInfo.FileInTask -> FileInfo.FileInTask(info.task, info.pathInTask)
      else -> null
    }
  }

  override fun beforePropertyChange(event: VirtualFilePropertyEvent) {
    if (event.propertyName != VirtualFile.PROP_NAME) return
    val newName = event.newValue as? String ?: return
    val (task, oldPath) = event.file.fileInfo(project) as? FileInfo.FileInTask ?: return
    val newPath = oldPath.replaceAfterLast(VfsUtilCore.VFS_SEPARATOR_CHAR, newName, newName)

    val taskFiles = task.taskFiles

    fun rename(oldPath: String, newPath: String) {
      val taskFile = taskFiles.remove(oldPath) ?: return
      taskFile.name = newPath
      task.addTaskFile(taskFile)
    }

    if (event.file.isDirectory) {
      val changedPaths = taskFiles.keys.filter { it.startsWith(oldPath) }
      for (oldObjectPath in changedPaths) {
        val newObjectPath = oldObjectPath.replaceFirst(oldPath, newPath)
        rename(oldObjectPath, newObjectPath)
      }
    } else {
      rename(oldPath, newPath)
    }

    StepikCourseChangeHandler.changed(task)
    YamlFormatSynchronizer.saveItem(task)
  }

  override fun fileInTaskCreated(fileInfo: FileInfo.FileInTask) {
    super.fileInTaskCreated(fileInfo)
    YamlFormatSynchronizer.saveItem(fileInfo.task)
    StepikCourseChangeHandler.changed(fileInfo.task)
  }

  override fun fileDeleted(event: VirtualFileEvent) {
    val fileInfo = event.file.fileInfo(project) ?: return
    val removedFile = event.file

    when (fileInfo) {
      is FileInfo.SectionDirectory -> deleteSection(fileInfo, removedFile)
      is FileInfo.LessonDirectory -> deleteLesson(fileInfo, removedFile)
      is FileInfo.TaskDirectory -> deleteTask(fileInfo, removedFile)
      is FileInfo.FileInTask -> deleteFileInTask(fileInfo, removedFile)
    }
  }

  private fun deleteLesson(info: FileInfo.LessonDirectory, removedLessonFile: VirtualFile) {
    val removedLesson = info.lesson
    val course = removedLesson.course
    val section = removedLesson.section
    val parentDir = removedLessonFile.parent
    if (section != null) {
      CCUtils.updateHigherElements(parentDir.children, Function { section.getLesson(it.name) }, removedLesson.index, -1)
      section.removeLesson(removedLesson)
      StepikCourseChangeHandler.contentChanged(section)
      YamlFormatSynchronizer.saveItem(section)
    } else {
      CCUtils.updateHigherElements(parentDir.children, Function { course.getItem(it.name) }, removedLesson.index, -1)
      course.removeLesson(removedLesson)
      StepikCourseChangeHandler.contentChanged(course)
      YamlFormatSynchronizer.saveItem(course)
    }
  }

  private fun deleteSection(info: FileInfo.SectionDirectory, removedFile: VirtualFile) {
    val removedSection = info.section
    val course = removedSection.course
    val parentDir = removedFile.parent
    CCUtils.updateHigherElements(parentDir.children, Function { course.getItem(it.name) }, removedSection.index, -1)
    course.removeSection(removedSection)
    YamlFormatSynchronizer.saveItem(course)
    StepikCourseChangeHandler.contentChanged(course)
  }

  private fun deleteTask(info: FileInfo.TaskDirectory, removedTask: VirtualFile) {
    val task = info.task
    val course = task.course
    val lessonDir = removedTask.parent ?: error("`$removedTask` parent shouldn't be null")
    val lesson = task.lesson
    CCUtils.updateHigherElements(lessonDir.children, Function { lesson.getTask(it.name) }, task.index, -1)
    lesson.getTaskList().remove(task)
    YamlFormatSynchronizer.saveItem(lesson)
    StepikCourseChangeHandler.contentChanged(lesson)

    val configurator = course.configurator
    if (configurator != null) {
      runInEdt { configurator.courseBuilder.refreshProject(project) }
    }
  }

  private fun deleteFileInTask(info: FileInfo.FileInTask, removedFile: VirtualFile) {
    val (task, pathInTask) = info

    val taskFiles = task.taskFiles
    if (removedFile.isDirectory) {
      val toRemove = taskFiles.keys.filter { it.startsWith(pathInTask) }
      for (path in toRemove) {
        taskFiles.remove(path)
      }
    } else {
      taskFiles.remove(pathInTask)
    }
    YamlFormatSynchronizer.saveItem(task)
    StepikCourseChangeHandler.changed(task)
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(CCVirtualFileListener::class.java)
  }
}
