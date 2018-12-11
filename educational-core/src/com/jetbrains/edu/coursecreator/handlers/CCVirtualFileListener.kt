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
import com.jetbrains.edu.learning.FileKind.*
import com.jetbrains.edu.learning.PlaceholderPainter
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.AdditionalFile
import com.jetbrains.edu.learning.courseFormat.StudyFile
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.fileInfo
import com.jetbrains.edu.learning.handlers.EduVirtualFileListener

class CCVirtualFileListener(project: Project) : EduVirtualFileListener(project) {

  override fun beforeFileMovement(event: VirtualFileMoveEvent) {
    val (task, oldPath) = event.file.fileInfo(project) as? FileInfo.FileInTask ?: return
    val (oldParentTask, oldParentPath) = event.oldParent.directoryFileInfo(project) ?: return
    val (newParentTask, newParentPath, newParentKind) = event.newParent.directoryFileInfo(project) ?: return

    if (oldParentTask != newParentTask) {
      LOG.warn("Unsupported case. `EduMoveDelegate` should forbid file moving between different tasks")
      return
    }

    val affectedFiles = mutableListOf<Pair<String, Any>>()

    fun <T> collectAffectedFiles(data: MutableMap<String, T>) {
      val oldPaths = data.keys.filter { it.startsWith(oldPath) }

      for (path in oldPaths) {
        val fileObject = data.remove(path) as? Any ?: continue
        if (fileObject is TaskFile && newParentKind != TASK_FILE) {
          PlaceholderPainter.hidePlaceholders(fileObject)
        }
        affectedFiles += path to fileObject
      }
    }

    collectAffectedFiles(task.taskFiles)
    collectAffectedFiles(task.testsText)
    collectAffectedFiles(task.additionalFiles)

    for ((oldFilePath, fileObject) in affectedFiles) {
      var newPath = oldFilePath.removePrefix("$oldParentPath/")
      if (newParentPath.isNotEmpty()) {
        newPath = "$newParentPath/$newPath"
      }
      when (newParentKind) {
        TASK_FILE -> task.addTaskFile(fileObject.toTaskFile(newPath))
        TEST_FILE -> task.testsText[newPath] = fileObject.toTestText()
        ADDITIONAL_FILE -> task.additionalFiles[newPath] = fileObject.toAdditionalFile()
      }
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
    val course = StudyTaskManager.getInstance(project).course ?: return null
    val sourceDir = course.sourceDir ?: return null
    val taskDirectoryKind = if (sourceDir.isEmpty()) TASK_FILE else ADDITIONAL_FILE

    val info = fileInfo(project) ?: return null
    return when (info) {
      is FileInfo.TaskDirectory -> FileInfo.FileInTask(info.task, "", taskDirectoryKind)
      is FileInfo.FileInTask -> FileInfo.FileInTask(info.task, info.pathInTask, info.kind)
      else -> null
    }
  }

  private fun Any.toTaskFile(newPath: String): TaskFile = when (this) {
    is TaskFile -> {
      name = newPath
      this
    }
    is AdditionalFile -> {
      val taskFile = TaskFile(newPath, getText())
      taskFile.isVisible = isVisible
      taskFile
    }
    is String -> TaskFile(newPath, this)
    else -> error("Unexpected object type: `${javaClass}`")
  }

  private fun Any.toTestText(): String = when (this) {
    is StudyFile -> getText()
    is String -> this
    else -> error("Unexpected object type: `${javaClass}`")
  }

  private fun Any.toAdditionalFile(): AdditionalFile = when (this) {
    is TaskFile -> AdditionalFile(getText(), isVisible)
    is AdditionalFile -> this
    is String -> AdditionalFile(this, true)
    else -> error("Unexpected object type: `${javaClass}`")
  }

  override fun beforePropertyChange(event: VirtualFilePropertyEvent) {
    if (event.propertyName != VirtualFile.PROP_NAME) return
    val newName = event.newValue as? String ?: return
    val (task, oldPath, kind) = event.file.fileInfo(project) as? FileInfo.FileInTask ?: return
    val newPath = oldPath.replaceAfterLast(VfsUtilCore.VFS_SEPARATOR_CHAR, newName, newName)

    fun <T> rename(container: MutableMap<String, T>, add: (String, T) -> Unit) {

      fun rename(oldPath: String, newPath: String) {
        val obj = container.remove(oldPath) ?: return
        add(newPath, obj)
      }

      if (event.file.isDirectory) {
        val changedPaths = container.keys.filter { it.startsWith(oldPath) }
        for (oldObjectPath in changedPaths) {
          val newObjectPath = oldObjectPath.replaceFirst(oldPath, newPath)
          rename(oldObjectPath, newObjectPath)
        }
      } else {
        rename(oldPath, newPath)
      }
    }

    when (kind) {
      TASK_FILE -> rename(task.taskFiles) { path, taskFile ->
        taskFile.name = path
        task.addTaskFile(taskFile)
      }
      TEST_FILE -> rename(task.testsText) { path, text ->
          task.addTestsTexts(path, text)
      }
      ADDITIONAL_FILE -> rename(task.additionalFiles) { path, additionalFile ->
          task.addAdditionalFile(path, additionalFile)
      }
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
    val (task, pathInTask, kind) = info

    fun <T> remove(data: MutableMap<String, T>) {
      val toRemove = data.keys.filter { it.startsWith(pathInTask) }
      for (path in toRemove) {
        data.remove(path)
      }
    }

    if (removedFile.isDirectory) {
      remove(task.taskFiles)
      remove(task.testsText)
      remove(task.additionalFiles)
    } else {
      when (kind) {
        TASK_FILE -> task.taskFiles.remove(pathInTask)
        TEST_FILE -> task.testsText.remove(pathInTask)
        ADDITIONAL_FILE -> task.additionalFiles.remove(pathInTask)
      }
    }
    YamlFormatSynchronizer.saveItem(task)
    StepikCourseChangeHandler.changed(task)
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(CCVirtualFileListener::class.java)
  }
}
