package com.jetbrains.edu.java.stepik.hyperskill

import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleConfiguratorBase
import com.jetbrains.edu.jvm.stepik.hyperskill.HyperskillGradleTaskCheckerProvider
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator

class JHyperskillConfigurator : GradleConfiguratorBase(), HyperskillConfigurator<JdkProjectSettings> {
  override fun getCourseBuilder() = JHyperskillCourseBuilder()
  override fun getTaskCheckerProvider() = HyperskillGradleTaskCheckerProvider()
  override fun isEnabled(): Boolean = true
}
