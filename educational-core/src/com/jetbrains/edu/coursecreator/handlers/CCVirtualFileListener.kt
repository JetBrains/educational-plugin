package com.jetbrains.edu.coursecreator.handlers

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.handlers.EduVirtualFileListener
import com.jetbrains.edu.learning.yaml.YamlFormatSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import com.jetbrains.edu.learning.yaml.YamlLoader

class CCVirtualFileListener(project: Project) : EduVirtualFileListener(project) {

  override fun taskFileCreated(taskFile: TaskFile, file: VirtualFile) {
    super.taskFileCreated(taskFile, file)
    if (file.isTestsFile(project) || file.isTaskRunConfigurationFile(project)) {
      taskFile.isVisible = false
    }
  }

  override fun fileDeleted(fileInfo: FileInfo, file: VirtualFile) {
    when (fileInfo) {
      is FileInfo.SectionDirectory -> deleteSection(fileInfo)
      is FileInfo.LessonDirectory -> deleteLesson(fileInfo)
      is FileInfo.TaskDirectory -> deleteTask(fileInfo)
      is FileInfo.FileInTask -> deleteFileInTask(fileInfo, file)
    }
  }

  private fun deleteLesson(info: FileInfo.LessonDirectory) {
    val removedLesson = info.lesson
    val course = removedLesson.course
    val section = removedLesson.section
    if (section != null) {
      section.removeLesson(removedLesson)
      YamlFormatSynchronizer.saveItem(section)
    } else {
      course.removeLesson(removedLesson)
      YamlFormatSynchronizer.saveItem(course)
    }
  }

  private fun deleteSection(info: FileInfo.SectionDirectory) {
    val removedSection = info.section
    val course = removedSection.course
    course.removeSection(removedSection)
    YamlFormatSynchronizer.saveItem(course)
  }

  private fun deleteTask(info: FileInfo.TaskDirectory) {
    val task = info.task
    val course = task.course
    val lesson = task.lesson
    lesson.removeTask(task)
    YamlFormatSynchronizer.saveItem(lesson)

    course.configurator?.courseBuilder?.refreshProject(project, RefreshCause.STRUCTURE_MODIFIED)
  }

  private fun deleteFileInTask(info: FileInfo.FileInTask, file: VirtualFile) {
    val (task, pathInTask) = info

    if (file.isDirectory) {
      val toRemove = task.taskFiles.keys.filter { it.startsWith(pathInTask) }
      for (path in toRemove) {
        task.removeTaskFile(path)
      }
    } else {
      task.removeTaskFile(pathInTask)
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

  override fun configUpdated(configEvents: List<VFileEvent>) {
    val sortedConfigEvents = configEvents.sortedWith(CompareConfigs)
    for (event in sortedConfigEvents) {
      when (event) {
        is VFileCreateEvent -> configCreated(event)
        is VFileContentChangeEvent -> configChanged(event)
      }
    }
  }

  private fun configChanged(event: VFileContentChangeEvent) {
    reloadConfig(event.file)
  }

  private fun configCreated(event: VFileCreateEvent) {
    val file = event.file ?: return
    reloadConfig(file)
  }

  private fun reloadConfig(file: VirtualFile) {
    if (file.length == 0L) return
    val loadFromConfig = file.getUserData(YamlFormatSynchronizer.LOAD_FROM_CONFIG) ?: true
    if (loadFromConfig) {
      runInEdt {
        YamlLoader.loadItem(project, file, true)
        ProjectView.getInstance(project).refresh()
      }
    }
  }

  private class CompareConfigs {
    companion object : Comparator<VFileEvent> {
      override fun compare(a: VFileEvent, b: VFileEvent): Int {
        return when(a.file?.name) {
          YamlFormatSettings.COURSE_CONFIG -> 0
          YamlFormatSettings.SECTION_CONFIG -> 1
          YamlFormatSettings.LESSON_CONFIG -> 2
          YamlFormatSettings.TASK_CONFIG -> 3
          else -> 4
        }
      }
    }
  }
}
