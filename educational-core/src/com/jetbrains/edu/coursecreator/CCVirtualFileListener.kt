package com.jetbrains.edu.coursecreator

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.util.Function
import com.jetbrains.edu.coursecreator.configuration.YamlFormatSynchronizer
import com.jetbrains.edu.coursecreator.stepik.StepikCourseChangeHandler
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.configurator

class CCVirtualFileListener(private val project: Project) : VirtualFileListener {

  override fun fileCreated(event: VirtualFileEvent) {
    val createdFile = event.file
    if (EduUtils.canBeAddedAsTaskFile(project, createdFile)) {
      val taskRelativePath = EduUtils.pathRelativeToTask(project, createdFile)
      val task = EduUtils.getTaskForFile(project, createdFile) ?: error("Task for `$createdFile` shouldn't be null here")
      task.addTaskFile(taskRelativePath)
      YamlFormatSynchronizer.saveItem(task)
      StepikCourseChangeHandler.changed(task)
    }
  }

  override fun fileDeleted(event: VirtualFileEvent) {
    val removedFile = event.file
    val path = removedFile.path
    if (path.contains(CCUtils.GENERATED_FILES_FOLDER)) return

    val courseDir = EduUtils.getCourseDir(project)
    if (!FileUtil.isAncestor(courseDir.path, removedFile.path, true)) return
    val course = StudyTaskManager.getInstance(project).course ?: return
    val taskFile = EduUtils.getTaskFile(project, removedFile)
    if (taskFile != null) {
      deleteTaskFile(project, removedFile, taskFile)
      return
    }
    if (EduUtils.isTaskDirectory(project, removedFile)) {
      val task = EduUtils.getTask(removedFile, course) ?: error("Task for `$removedFile` shouldn't be null here")
      StepikCourseChangeHandler.contentChanged(task.lesson)
      deleteTask(course, removedFile)
      val configurator = course.configurator
      if (configurator != null) {
        ApplicationManager.getApplication().invokeLater { configurator.courseBuilder.refreshProject(project) }
      }
    }
    if (EduUtils.getLesson(removedFile, course) != null) {
      val removedLesson = EduUtils.getLesson(removedFile, course) ?: return

      val section = removedLesson.section
      if (section == null) {
        StepikCourseChangeHandler.contentChanged(course)
      } else {
        StepikCourseChangeHandler.contentChanged(section)
      }

      deleteLesson(course, removedFile)
    }
    if (course.getSection(removedFile.name) != null) {
      deleteSection(course, removedFile)
      StepikCourseChangeHandler.contentChanged(course)
    }
  }

  private fun deleteLesson(course: Course, removedLessonFile: VirtualFile) {
    val removedLesson = EduUtils.getLesson(removedLessonFile, course) ?: return
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

  private fun deleteSection(course: Course, removedFile: VirtualFile) {
    val removedSection = course.getSection(removedFile.name) ?: return
    val parentDir = removedFile.parent
    CCUtils.updateHigherElements(parentDir.children, Function { course.getItem(it.name) }, removedSection.index, -1)
    course.removeSection(removedSection)
    YamlFormatSynchronizer.saveItem(course)
  }

  private fun deleteTask(course: Course, removedTask: VirtualFile) {
    val lessonDir = removedTask.parent ?: error("`$removedTask` parent shouldn't be null")
    val lesson = EduUtils.getLesson(lessonDir, course) ?: error("Lesson for `$lessonDir` shouldn't be null here")
    val task = lesson.getTask(removedTask.name) ?: return
    CCUtils.updateHigherElements(lessonDir.children, Function { lesson.getTask(it.name) }, task.index, -1)
    lesson.getTaskList().remove(task)
    StepikCourseChangeHandler.contentChanged(lesson)
    YamlFormatSynchronizer.saveItem(lesson)
  }

  private fun deleteTaskFile(project: Project, removedTaskFile: VirtualFile, taskFile: TaskFile) {
    val task = taskFile.task ?: return
    task.getTaskFiles().remove(EduUtils.pathRelativeToTask(project, removedTaskFile))
    YamlFormatSynchronizer.saveItem(task)
    StepikCourseChangeHandler.changed(task)

  }
}
