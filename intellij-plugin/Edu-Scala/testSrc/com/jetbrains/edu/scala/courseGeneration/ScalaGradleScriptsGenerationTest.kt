package com.jetbrains.edu.scala.courseGeneration

import com.jetbrains.edu.jvm.courseGeneration.GradleScriptsGenerationTestBase
import com.jetbrains.edu.learning.CourseBuilder
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.scala.gradle.ScalaGradleCourseBuilder.Companion.SCALA_BUILD_GRADLE_TEMPLATE_NAME
import org.jetbrains.plugins.scala.ScalaLanguage

class ScalaGradleScriptsGenerationTest : GradleScriptsGenerationTestBase() {
  override val defaultBuildGradleTemplateName: String = SCALA_BUILD_GRADLE_TEMPLATE_NAME

  override fun createCourse(courseMode: CourseMode, buildCourse: CourseBuilder.() -> Unit): Course {
    return course(courseMode = courseMode, language = ScalaLanguage.INSTANCE, environment = "Gradle", buildCourse = buildCourse)
  }
}