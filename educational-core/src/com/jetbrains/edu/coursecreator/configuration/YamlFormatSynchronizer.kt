package com.jetbrains.edu.coursecreator.configuration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task


object YamlFormatSynchronizer {
  private val LOG = Logger.getInstance(YamlFormatSynchronizer.javaClass)

  @JvmStatic
  fun saveItem(item: StudyItem, project: Project) {
    if (YamlFormatSettings.isDisabled()) {
      return
    }
    if (item.course.isStudy) {
      return
    }
    val fileName = when (item) {
      is Course -> YamlFormatSettings.COURSE_CONFIG
      is Section -> YamlFormatSettings.SECTION_CONFIG
      is Lesson -> YamlFormatSettings.LESSON_CONFIG
      is Task -> YamlFormatSettings.TASK_CONFIG
      else -> error("Unknown StudyItem type: ${item.javaClass.name}")
    }
    val dir = item.getDir(project)
    if (dir == null) {
      LOG.error("Failed to save ${item.javaClass.name} '${item.name}' to config file: directory not found")
      return
    }
    ApplicationManager.getApplication().runWriteAction {
      dir.findOrCreateChildData(YamlFormatSynchronizer.javaClass, fileName)
    }
  }

  @JvmStatic
  fun saveAll(project: Project) {
    val course = StudyTaskManager.getInstance(project).course
    if (course == null) {
      LOG.error("Attempt to create config files for project without course")
      return
    }
    saveItem(course, project)
    for (item in course.items) {
      saveItem(item, project)
    }
    course.visitLessons(LessonVisitor { lesson ->
      for (task in lesson.getTaskList()) {
        saveItem(task, project)
      }
      true
    })
  }
}