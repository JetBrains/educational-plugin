package com.jetbrains.edu.java.stepik.hyperskill

import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleConfiguratorBase
import com.jetbrains.edu.jvm.stepik.fileName
import com.jetbrains.edu.jvm.stepik.hyperskill.HyperskillGradleTaskCheckerProvider
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator

class JHyperskillConfigurator : GradleConfiguratorBase(), HyperskillConfigurator<JdkProjectSettings> {
  override fun getCourseBuilder() = JHyperskillCourseBuilder()
  override fun getTaskCheckerProvider() = HyperskillGradleTaskCheckerProvider()
  override fun isEnabled(): Boolean = true
  override fun getMockFileName(text: String) = fileName(JavaLanguage.INSTANCE, text)
}
