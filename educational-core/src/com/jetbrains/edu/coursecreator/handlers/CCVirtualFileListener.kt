package com.jetbrains.edu.coursecreator.handlers

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.util.Function
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.FileInfo
import com.jetbrains.edu.learning.RefreshCause
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.fileInfo
import com.jetbrains.edu.learning.handlers.EduVirtualFileListener
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

class CCVirtualFileListener(project: Project) : EduVirtualFileListener(project) {

  override fun taskFileCreated(taskFile: TaskFile, file: VirtualFile) {
    super.taskFileCreated(taskFile, file)
    if (EduUtils.isTestsFile(project, file)) {
      taskFile.isVisible = false
    }
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
      YamlFormatSynchronizer.saveItem(section)
    } else {
      CCUtils.updateHigherElements(parentDir.children, Function { course.getItem(it.name) }, removedLesson.index, -1)
      course.removeLesson(removedLesson)
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
  }

  private fun deleteTask(info: FileInfo.TaskDirectory, removedTask: VirtualFile) {
    val task = info.task
    val course = task.course
    val lessonDir = removedTask.parent ?: error("`$removedTask` parent shouldn't be null")
    val lesson = task.lesson
    CCUtils.updateHigherElements(lessonDir.children, Function { lesson.getTask(it.name) }, task.index, -1)
    lesson.removeTask(task)
    YamlFormatSynchronizer.saveItem(lesson)

    val configurator = course.configurator
    if (configurator != null) {
      runInEdt { configurator.courseBuilder.refreshProject(project, RefreshCause.STRUCTURE_MODIFIED) }
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
  }


}
