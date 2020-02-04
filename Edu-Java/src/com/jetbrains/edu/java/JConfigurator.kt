package com.jetbrains.edu.java

import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.jvm.gradle.GradleConfiguratorBase
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.jvm.stepik.fileName
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import icons.EducationalCoreIcons
import javax.swing.Icon

class JConfigurator : GradleConfiguratorBase() {
  override val courseBuilder: GradleCourseBuilderBase
    get() = JCourseBuilder()

  override val testFileName: String
    get() = TEST_JAVA

  override val isEnabled: Boolean
    get() = !EduUtils.isAndroidStudio()

  override val taskCheckerProvider: TaskCheckerProvider
    get() = JTaskCheckerProvider()

  override fun getMockFileName(text: String): String = fileName(JavaLanguage.INSTANCE, text)

  override val mockTemplate: String
    get() = getInternalTemplateText(MOCK_JAVA)

  override val logo: Icon
    get() = EducationalCoreIcons.JavaLogo

  companion object {
    const val TEST_JAVA = "Tests.java"
    const val TASK_JAVA = "Task.java"
    const val MOCK_JAVA = "Mock.java"
  }
}