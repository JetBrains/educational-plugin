package com.jetbrains.edu.scala.hyperskill

import com.google.common.annotations.VisibleForTesting
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleHyperskillConfigurator
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.scala.gradle.ScalaGradleConfigurator
import com.jetbrains.edu.scala.gradle.ScalaGradleCourseBuilder

class ScalaHyperskillConfigurator : GradleHyperskillConfigurator<JdkProjectSettings>(ScalaGradleConfigurator()) {
  override val courseBuilder: EduCourseBuilder<JdkProjectSettings>
    get() = ScalaHyperskillCourseBuilder()

  private class ScalaHyperskillCourseBuilder : ScalaGradleCourseBuilder() {
    override val buildGradleTemplateName: String = SCALA_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME
  }

  companion object {
    @VisibleForTesting
    const val SCALA_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME = "hyperskill-scala-build.gradle"
  }
}
