package com.jetbrains.edu.android

import com.jetbrains.edu.jvm.gradle.GradleConfiguratorBase
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.isUnitTestMode
import icons.AndroidIcons
import javax.swing.Icon

class AndroidConfigurator : GradleConfiguratorBase() {
  override val courseBuilder: GradleCourseBuilderBase = AndroidCourseBuilder()
  override val taskCheckerProvider: TaskCheckerProvider = AndroidTaskCheckerProvider()
  override val sourceDir: String = "src/main"
  override val testDirs: List<String> = listOf("src/test", "src/androidTest")
  override val testFileName: String = "ExampleUnitTest.kt"
  override val isEnabled: Boolean = isFeatureEnabled(EduExperimentalFeatures.ANDROID_COURSES) || isUnitTestMode
  override val logo: Icon = AndroidIcons.Android
}
