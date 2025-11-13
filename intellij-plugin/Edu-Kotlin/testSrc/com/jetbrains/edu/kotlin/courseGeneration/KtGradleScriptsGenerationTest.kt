package com.jetbrains.edu.kotlin.courseGeneration

import com.jetbrains.edu.jvm.courseGeneration.GradleScriptsGenerationTestBase
import com.jetbrains.edu.kotlin.KtCourseBuilder.Companion.KOTLIN_BUILD_GRADLE_TEMPLATE_NAME
import com.jetbrains.edu.learning.CourseBuilder
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import org.jetbrains.kotlin.idea.KotlinLanguage

class KtGradleScriptsGenerationTest : GradleScriptsGenerationTestBase() {
  override val defaultBuildGradleTemplateName: String = KOTLIN_BUILD_GRADLE_TEMPLATE_NAME

  override fun createCourse(courseMode: CourseMode, buildCourse: CourseBuilder.() -> Unit): Course {
    return course(courseMode = courseMode, language = KotlinLanguage.INSTANCE, buildCourse = buildCourse)
  }
}