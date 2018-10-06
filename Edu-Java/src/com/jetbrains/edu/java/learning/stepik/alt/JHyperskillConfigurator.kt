package com.jetbrains.edu.java.learning.stepik.alt

import com.jetbrains.edu.java.learning.JTaskCheckerProvider
import com.jetbrains.edu.learning.gradle.GradleConfiguratorBase

class JHyperskillConfigurator : GradleConfiguratorBase() {

  override fun getCourseBuilder() = JHyperskillCourseBuilder()
  override fun getTestFileName() = ""
  override fun getTaskCheckerProvider() = JTaskCheckerProvider()
}
