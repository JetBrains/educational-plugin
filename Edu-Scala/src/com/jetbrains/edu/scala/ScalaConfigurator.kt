package com.jetbrains.edu.scala

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.intellij.GradleConfiguratorBase
import com.jetbrains.edu.learning.intellij.JdkProjectSettings

class ScalaConfigurator : GradleConfiguratorBase() {

  private val myCourseBuilder = ScalaCourseBuilder()

  override fun getCourseBuilder(): EduCourseBuilder<JdkProjectSettings> = myCourseBuilder

  override fun getTestFileName(): String = TEST_SCALA

  override fun isTestFile(file: VirtualFile): Boolean = TEST_SCALA == file.name

  override fun isEnabled(): Boolean = !EduUtils.isAndroidStudio()

  override fun getTaskCheckerProvider(): TaskCheckerProvider = ScalaTaskCheckerProvider()

  override fun getMockTemplate(): String = FileTemplateManager.getDefaultInstance().getInternalTemplate(MOCK_SCALA).text

  companion object {
    const val TEST_SCALA = "Test.scala"
    const val TASK_SCALA = "Task.scala"
    const val MOCK_SCALA = "Mock.scala"
  }
}
