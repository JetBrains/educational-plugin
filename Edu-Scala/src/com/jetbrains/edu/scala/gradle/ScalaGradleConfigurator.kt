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
  override val courseBuilder: ScalaGradleCourseBuilder = ScalaGradleCourseBuilder()
  override val testFileName: String = TEST_SCALA
  override val isEnabled: Boolean = !EduUtils.isAndroidStudio()
  override val taskCheckerProvider: TaskCheckerProvider = ScalaGradleTaskCheckerProvider()
  override val mockTemplate: String = getInternalTemplateText(MOCK_SCALA)
  override fun getMockFileName(text: String): String = fileName(ScalaLanguage.INSTANCE, text)
  override val logo: Icon = EducationalCoreIcons.ScalaLogo

  companion object {
    const val TEST_SCALA = "Test.scala"
    const val TASK_SCALA = "Task.scala"
    const val MOCK_SCALA = "Mock.scala"
  }
}
