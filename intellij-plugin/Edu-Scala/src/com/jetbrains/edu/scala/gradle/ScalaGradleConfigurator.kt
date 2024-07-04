package com.jetbrains.edu.scala.gradle

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.jvm.gradle.GradleConfiguratorBase
import com.jetbrains.edu.jvm.gradle.checker.GradleTaskCheckerProvider
import com.jetbrains.edu.jvm.stepik.fileName
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.scala.isScalaPluginCompatible
import org.jetbrains.plugins.scala.ScalaLanguage
import javax.swing.Icon

class ScalaGradleConfigurator : GradleConfiguratorBase() {
  override val courseBuilder: ScalaGradleCourseBuilder
    get() = ScalaGradleCourseBuilder()

  override val testFileName: String
    get() = TEST_SCALA

  override val isEnabled: Boolean
    get() = !EduUtilsKt.isAndroidStudio() && isScalaPluginCompatible

  override val taskCheckerProvider: TaskCheckerProvider
    get() = GradleTaskCheckerProvider()

  override val mockTemplate: String
    get() = getInternalTemplateText(MOCK_SCALA)

  override fun getMockFileName(course: Course, text: String): String = fileName(ScalaLanguage.INSTANCE, text)

  override val logo: Icon
    get() = EducationalCoreIcons.ScalaLogo

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

  companion object {
    const val TEST_SCALA = "Test.scala"
    const val TASK_SCALA = "Task.scala"
    const val MAIN_SCALA = "Main.scala"
    const val MOCK_SCALA = "Mock.scala"
  }
}
