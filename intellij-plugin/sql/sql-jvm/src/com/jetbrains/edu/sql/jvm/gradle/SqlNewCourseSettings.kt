package com.jetbrains.edu.sql.jvm.gradle

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.newCourseSettings.NewCourseSettings
import com.jetbrains.edu.learning.newproject.newCourseSettings.NewCourseSettingsCatalog

class SqlNewCourseSettings(val testLanguage: SqlTestLanguage): NewCourseSettings {
  override fun applyToCourse(course: Course) {
    course.sqlTestLanguage = testLanguage
  }

  fun isAvailable(): Boolean = testLanguage.getLanguage() != null
}

object SqlNewCourseSettingsCatalog : NewCourseSettingsCatalog.List<SqlNewCourseSettings> {
  private val Kotlin = SqlNewCourseSettings(SqlTestLanguage.KOTLIN)
  private val Java = SqlNewCourseSettings(SqlTestLanguage.JAVA)

  override val preferred: SqlNewCourseSettings = Kotlin.takeIf { it.isAvailable() } ?: Java

  override val configs: List<SqlNewCourseSettings>
    get() = listOf(Kotlin, Java).filter { it.isAvailable() }
}