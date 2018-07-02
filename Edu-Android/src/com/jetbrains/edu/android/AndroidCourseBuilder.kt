package com.jetbrains.edu.android

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.intellij.GradleCourseBuilderBase

class AndroidCourseBuilder : GradleCourseBuilderBase() {

  override val buildGradleTemplateName: String = "android-build.gradle"

  override fun getConfigVariables(project: Project): Map<String, String> {
    // TODO: extract suitable android gradle plugin version from android plugin
    // TODO: use com.jetbrains.edu.kotlin.KtCourseBuilder.Companion#getKotlinPluginVersion
    return super.getConfigVariables(project) + mapOf("ANDROID_GRADLE_PLUGIN_VERSION" to "3.1.3", "KOTLIN_VERSION" to "1.2.50")
  }

  override fun createInitialLesson(project: Project, course: Course): Lesson? = null
}
