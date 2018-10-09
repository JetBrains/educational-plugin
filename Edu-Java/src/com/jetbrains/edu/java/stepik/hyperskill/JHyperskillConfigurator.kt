package com.jetbrains.edu.java.stepik.hyperskill

import com.jetbrains.edu.java.JTaskCheckerProvider
import com.jetbrains.edu.learning.gradle.GradleConfiguratorBase

class JHyperskillConfigurator : GradleConfiguratorBase() {

  override fun getCourseBuilder() = JHyperskillCourseBuilder()
  override fun getTestFileName() = ""
  override fun getTaskCheckerProvider() = JTaskCheckerProvider()
}
