package com.jetbrains.edu.coursecreator.handlers

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.*
import com.intellij.util.Function
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.configuration.YamlFormatSynchronizer
import com.jetbrains.edu.coursecreator.stepik.StepikCourseChangeHandler
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.handlers.EduVirtualFileListener
import com.jetbrains.edu.learning.handlers.EduVirtualFileListener.FileKind.*

class CCVirtualFileListener(project: Project) : EduVirtualFileListener(project) {

  override fun beforePropertyChange(event: VirtualFilePropertyEvent) {
    if (event.propertyName != VirtualFile.PROP_NAME) return
    val newName = event.newValue as? String ?: return
    val (task, oldPath, kind) = event.fileInfo(project) as? FileInfo.FileInTask ?: return
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

  override fun fileInTaskCreated(event: VirtualFileEvent, fileInfo: FileInfo.FileInTask) {
    super.fileInTaskCreated(event, fileInfo)
    YamlFormatSynchronizer.saveItem(fileInfo.task)
    StepikCourseChangeHandler.changed(fileInfo.task)
  }

  override fun fileDeleted(event: VirtualFileEvent) {
    val fileInfo = event.fileInfo(project) ?: return
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
}
