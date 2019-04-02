package com.jetbrains.edu.scala

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.jetbrains.edu.jvm.gradle.GradleConfiguratorBase
import com.jetbrains.edu.jvm.stepik.fileName
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import icons.EducationalCoreIcons
import org.jetbrains.plugins.scala.ScalaLanguage
import javax.swing.Icon

class ScalaConfigurator : GradleConfiguratorBase() {

  private val myCourseBuilder = ScalaCourseBuilder()

  override fun getCourseBuilder() = myCourseBuilder

  override fun getTestFileName(): String = TEST_SCALA

  override fun isEnabled(): Boolean = !EduUtils.isAndroidStudio()

  override fun getTaskCheckerProvider(): TaskCheckerProvider = ScalaTaskCheckerProvider()

  override fun getMockTemplate(): String = FileTemplateManager.getDefaultInstance().getInternalTemplate(MOCK_SCALA).text

  override fun getMockFileName(text: String): String = fileName(ScalaLanguage.INSTANCE, text)

  override fun getLogo(): Icon = EducationalCoreIcons.ScalaLogo

  companion object {
    const val TEST_SCALA = "Test.scala"
    const val TASK_SCALA = "Task.scala"
    const val MOCK_SCALA = "Mock.scala"
  }
}
