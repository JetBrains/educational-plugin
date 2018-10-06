package com.jetbrains.edu.java.learning.stepik.alt

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.gradle.generation.GradleCourseProjectGenerator

class JHyperskillCourseBuilder : GradleCourseBuilderBase() {

  override val buildGradleTemplateName: String = "java-build.gradle"

  override fun getTaskTemplateName(): String? = null
  override fun getTestTemplateName(): String? = null
  override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator {
    return JHyperskillCourseProjectGenerator(this, course)
  }
}
