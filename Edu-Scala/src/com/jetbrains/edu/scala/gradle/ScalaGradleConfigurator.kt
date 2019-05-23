package com.jetbrains.edu.scala.gradle

import com.jetbrains.edu.jvm.gradle.GradleConfiguratorBase
import com.jetbrains.edu.jvm.stepik.fileName
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import icons.EducationalCoreIcons
import org.jetbrains.plugins.scala.ScalaLanguage
import javax.swing.Icon

class ScalaGradleConfigurator : GradleConfiguratorBase() {

  private val myCourseBuilder = ScalaGradleCourseBuilder()

  override fun getCourseBuilder() = myCourseBuilder

  override fun getTestFileName(): String = TEST_SCALA

  override fun isEnabled(): Boolean = !EduUtils.isAndroidStudio()

  override fun getTaskCheckerProvider(): TaskCheckerProvider = ScalaGradleTaskCheckerProvider()

  override fun getMockTemplate(): String = getInternalTemplateText(MOCK_SCALA)

  override fun getMockFileName(text: String): String = fileName(ScalaLanguage.INSTANCE, text)

  override fun getLogo(): Icon = EducationalCoreIcons.ScalaLogo

  companion object {
    const val TEST_SCALA = "Test.scala"
    const val TASK_SCALA = "Task.scala"
    const val MOCK_SCALA = "Mock.scala"
  }
}
