package com.jetbrains.edu.sql

import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class SqlCourseBuilder : EduCourseBuilder<Unit> {

  override val taskTemplateName: String get() = TASK_SQL

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<Unit> = SqlCourseProjectGenerator(this, course)
  override fun getLanguageSettings(): LanguageSettings<Unit> = SqlLanguageSettings()

  companion object {
    const val TASK_SQL = "task.sql"
  }
}

private class SqlLanguageSettings : LanguageSettings<Unit>() {
  override fun getSettings() = Unit
}
