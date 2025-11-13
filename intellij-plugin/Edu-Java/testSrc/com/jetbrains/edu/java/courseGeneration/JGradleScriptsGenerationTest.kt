package com.jetbrains.edu.java.courseGeneration

import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.java.JCourseBuilder.Companion.JAVA_BUILD_GRADLE_TEMPLATE_NAME
import com.jetbrains.edu.jvm.courseGeneration.GradleScriptsGenerationTestBase
import com.jetbrains.edu.learning.CourseBuilder
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode

class JGradleScriptsGenerationTest : GradleScriptsGenerationTestBase() {
  override val defaultBuildGradleTemplateName: String = JAVA_BUILD_GRADLE_TEMPLATE_NAME

  override fun createCourse(courseMode: CourseMode, buildCourse: CourseBuilder.() -> Unit): Course {
    return course(courseMode = courseMode, language = JavaLanguage.INSTANCE, buildCourse = buildCourse)
  }
}