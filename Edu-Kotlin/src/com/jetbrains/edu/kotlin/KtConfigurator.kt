package com.jetbrains.edu.kotlin

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.jetbrains.edu.kotlin.checker.KtTaskCheckerProvider
import com.jetbrains.edu.learning.gradle.GradleConfiguratorBase

open class KtConfigurator : GradleConfiguratorBase() {

  private val myCourseBuilder = KtCourseBuilder()

  override fun getCourseBuilder() = myCourseBuilder

  override fun getTestFileName(): String = TESTS_KT

  override fun getTaskCheckerProvider() = KtTaskCheckerProvider()

  override fun getMockTemplate(): String {
    return FileTemplateManager.getDefaultInstance().getInternalTemplate(MOCK_KT).text
  }

  companion object {
    const val TESTS_KT = "Tests.kt"
    const val TASK_KT = "Task.kt"
    const val MOCK_KT = "Mock.kt"
  }
}
