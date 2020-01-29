package com.jetbrains.edu.kotlin

import com.jetbrains.edu.jvm.gradle.GradleConfiguratorBase
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.kotlin.checker.KtTaskCheckerProvider
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import org.jetbrains.kotlin.idea.KotlinIcons
import javax.swing.Icon

open class KtConfigurator : GradleConfiguratorBase() {
  override val courseBuilder: GradleCourseBuilderBase = KtCourseBuilder()
  override val testFileName: String = TESTS_KT
  override fun getMockFileName(text: String): String = TASK_KT
  override val taskCheckerProvider: KtTaskCheckerProvider = KtTaskCheckerProvider()
  override val mockTemplate: String = getInternalTemplateText(MOCK_KT)
  override val logo: Icon = KotlinIcons.SMALL_LOGO

  companion object {
    const val TESTS_KT = "Tests.kt"
    const val TASK_KT = "Task.kt"
    const val MOCK_KT = "Mock.kt"
  }
}
