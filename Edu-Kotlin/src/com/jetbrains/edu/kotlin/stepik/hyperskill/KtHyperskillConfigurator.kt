package com.jetbrains.edu.kotlin.stepik.hyperskill

import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleConfiguratorBase
import com.jetbrains.edu.jvm.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.jvm.stepik.hyperskill.HyperskillGradleCourseProjectGenerator
import com.jetbrains.edu.jvm.stepik.hyperskill.HyperskillGradleTaskCheckerProvider
import com.jetbrains.edu.kotlin.KtCourseBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator

class KtHyperskillConfigurator : GradleConfiguratorBase(), HyperskillConfigurator<JdkProjectSettings> {
  override fun getCourseBuilder() = object: KtCourseBuilder() {
      override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator {
        return HyperskillGradleCourseProjectGenerator(this, course)
      }
  }
  override fun getTaskCheckerProvider() = HyperskillGradleTaskCheckerProvider()
}
