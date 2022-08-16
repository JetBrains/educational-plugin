package com.jetbrains.edu.sql.jvm.gradle

import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.jvm.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.sql.core.SqlConfiguratorBase

abstract class SqlGradleCourseBuilderBase : GradleCourseBuilderBase() {
  override val taskTemplateName: String
    get() = SqlConfiguratorBase.TASK_SQL

  override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator {
    return SqlGradleCourseProjectGenerator(this, course)
  }
}
