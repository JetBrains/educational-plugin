package com.jetbrains.edu.learning.stepik.alt

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.gradle.generation.GradleCourseProjectGenerator

class HyperskillCourseBuilder : GradleCourseBuilderBase() {

  override val buildGradleTemplateName: String = "java-build.gradle"

  override fun getTaskTemplateName(): String? = null

  override fun getTestTemplateName(): String? = null
  override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator {
    return HyperskillCourseProjectGenerator(this, course)
  }
}
