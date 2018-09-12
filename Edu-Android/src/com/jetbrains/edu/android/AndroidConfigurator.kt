package com.jetbrains.edu.android

import com.intellij.openapi.application.Experiments
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.gradle.GradleConfiguratorBase
import com.jetbrains.edu.learning.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.isUnitTestMode

class AndroidConfigurator : GradleConfiguratorBase() {

  private val courseBuilder: AndroidCourseBuilder = AndroidCourseBuilder()
  private val taskCheckerProvider: AndroidTaskCheckerProvider = AndroidTaskCheckerProvider()

  override fun getCourseBuilder(): GradleCourseBuilderBase = courseBuilder

  override fun getSourceDir(): String = "src/main"
  override fun getTestDirs(): List<String> = listOf("src/test", "src/androidTest")

  // TODO: get rid of this method at all
  override fun getTestFileName(): String = "ExampleUnitTest.kt"

  override fun getTaskCheckerProvider(): TaskCheckerProvider = taskCheckerProvider

  override fun isEnabled(): Boolean = Experiments.isFeatureEnabled(EduExperimentalFeatures.ANDROID_COURSES) || isUnitTestMode
}
