package com.jetbrains.edu.android

import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.intellij.GradleConfiguratorBase
import com.jetbrains.edu.learning.intellij.GradleCourseBuilderBase

class AndroidConfigurator : GradleConfiguratorBase() {

  private val courseBuilder: AndroidCourseBuilder = AndroidCourseBuilder()

  override fun getCourseBuilder(): GradleCourseBuilderBase = courseBuilder

  override fun getSourceDir(): String = "src/main"
  override fun getTestDir(): String = "src/test"

  override fun getTestFileName(): String {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getTaskCheckerProvider(): TaskCheckerProvider {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}
