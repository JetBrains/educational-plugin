package com.jetbrains.edu.sql.jvm.gradle

import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.jvm.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.courseFormat.Course

class SqlGradleCourseProjectGenerator(
  builder: GradleCourseBuilderBase,
  course: Course
) : GradleCourseProjectGenerator(builder, course) {

  override fun applySettings(projectSettings: JdkProjectSettings) {
    super.applySettings(projectSettings)
    if (projectSettings is SqlJdkProjectSettings && projectSettings.testLanguage != null) {
      course.sqlTestLanguage = projectSettings.testLanguage
    }
  }
}
