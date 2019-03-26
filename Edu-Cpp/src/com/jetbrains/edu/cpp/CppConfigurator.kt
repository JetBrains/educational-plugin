package com.jetbrains.edu.cpp

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.jetbrains.edu.cpp.checker.CppTaskCheckerProvider
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator

class CppConfigurator : EduConfigurator<CppProjectSettings> {

  private val builder: CppCourseBuilder = CppCourseBuilder()
  private val taskCheckerProvider: CppTaskCheckerProvider = CppTaskCheckerProvider()

  override fun getTaskCheckerProvider(): TaskCheckerProvider = taskCheckerProvider

  override fun getTestFileName(): String = TESTS_CPP

  override fun getMockFileName(text: String): String = TASK_CPP

  override fun getCourseBuilder(): EduCourseBuilder<CppProjectSettings> = builder

  override fun getSourceDir(): String = EduNames.SRC

  override fun getMockTemplate(): String = FileTemplateManager.getDefaultInstance().getInternalTemplate(MOCK_CPP).text

  override fun isCourseCreatorEnabled(): Boolean = false

  companion object {
    private const val TASK_CPP = "task.cpp"
    private const val TESTS_CPP = "tests.cpp"
    private const val MOCK_CPP = "Mock.cpp"
  }
}