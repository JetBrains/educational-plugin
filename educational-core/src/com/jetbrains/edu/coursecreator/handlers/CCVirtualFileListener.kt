package com.jetbrains.edu.coursecreator.handlers

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.util.Function
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.handlers.EduVirtualFileListener
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

class CCVirtualFileListener(project: Project) : EduVirtualFileListener(project) {

  override fun taskFileCreated(taskFile: TaskFile, file: VirtualFile) {
    super.taskFileCreated(taskFile, file)
    if (file.isTestsFile(project) || file.isTaskRunConfigurationFile(project)) {
      taskFile.isVisible = false
    }
  }

  override fun fileDeleted(fileInfo: FileInfo, file: VirtualFile) {
    when (fileInfo) {
      is FileInfo.SectionDirectory -> deleteSection(fileInfo, file)
      is FileInfo.LessonDirectory -> deleteLesson(fileInfo, file)
      is FileInfo.TaskDirectory -> deleteTask(fileInfo, file)
      is FileInfo.FileInTask -> deleteFileInTask(fileInfo, file)
    }
  }

  private fun deleteLesson(info: FileInfo.LessonDirectory, file: VirtualFile) {
    val removedLesson = info.lesson
    val course = removedLesson.course
    val section = removedLesson.section
    val parentDir = file.parent
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

  private fun deleteSection(info: FileInfo.SectionDirectory, file: VirtualFile) {
    val removedSection = info.section
    val course = removedSection.course
    val parentDir = file.parent
    CCUtils.updateHigherElements(parentDir.children, Function { course.getItem(it.name) }, removedSection.index, -1)
    course.removeSection(removedSection)
    YamlFormatSynchronizer.saveItem(course)
  }

  private fun deleteTask(info: FileInfo.TaskDirectory, file: VirtualFile) {
    val task = info.task
    val course = task.course
    val lessonDir = file.parent ?: error("`$file` parent shouldn't be null")
    val lesson = task.lesson
    CCUtils.updateHigherElements(lessonDir.children, Function { lesson.getTask(it.name) }, task.index, -1)
    lesson.removeTask(task)
    YamlFormatSynchronizer.saveItem(lesson)

    course.configurator?.courseBuilder?.refreshProject(project, RefreshCause.STRUCTURE_MODIFIED)
  }

  private fun deleteFileInTask(info: FileInfo.FileInTask, file: VirtualFile) {
    val (task, pathInTask) = info

    val taskFiles = task.taskFiles
    if (file.isDirectory) {
      val toRemove = taskFiles.keys.filter { it.startsWith(pathInTask) }
      for (path in toRemove) {
        taskFiles.remove(path)
      }
    } else {
      taskFiles.remove(pathInTask)
    }
    YamlFormatSynchronizer.saveItem(task)
  }

  override fun beforeFileDeletion(event: VFileDeleteEvent) {
    val fileInfo = event.file.fileInfo(project) ?: return

    val studyItem = when (fileInfo) {
      is FileInfo.SectionDirectory -> fileInfo.section
      is FileInfo.LessonDirectory -> fileInfo.lesson
      is FileInfo.TaskDirectory -> fileInfo.task
      else -> return
    }

    val courseBuilder = studyItem.course.configurator?.courseBuilder ?: return
    courseBuilder.beforeStudyItemDeletion(project, studyItem)
  }
}
