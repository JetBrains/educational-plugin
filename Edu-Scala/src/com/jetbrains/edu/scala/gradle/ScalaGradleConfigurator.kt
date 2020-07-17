package com.jetbrains.edu.scala.gradle

import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.gradle.GradleConfiguratorBase
import com.jetbrains.edu.jvm.gradle.checker.GradleTaskCheckerProvider
import com.jetbrains.edu.jvm.stepik.fileName
import com.jetbrains.edu.jvm.stepik.findCodeTaskFile
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import icons.EducationalCoreIcons
import org.jetbrains.plugins.scala.ScalaLanguage
import javax.swing.Icon

class ScalaGradleConfigurator : GradleConfiguratorBase() {
  override val courseBuilder: ScalaGradleCourseBuilder
    get() = ScalaGradleCourseBuilder()

  override val testFileName: String
    get() = TEST_SCALA

  override val isEnabled: Boolean
    get() = !EduUtils.isAndroidStudio()

  override val taskCheckerProvider: GradleTaskCheckerProvider
    get() = ScalaGradleTaskCheckerProvider()

  override val mockTemplate: String
    get() = getInternalTemplateText(MOCK_SCALA)

  override fun getMockFileName(text: String): String = fileName(ScalaLanguage.INSTANCE, text)

  override fun getCodeTaskFile(project: Project, task: Task): TaskFile? {
    return findCodeTaskFile(project, task, taskCheckerProvider::mainClassForFile) ?: super.getCodeTaskFile(project, task)
  }

  override val logo: Icon
    get() = EducationalCoreIcons.ScalaLogo

  companion object {
    const val TEST_SCALA = "Test.scala"
    const val TASK_SCALA = "Task.scala"
    const val MAIN_SCALA = "Main.scala"
    const val MOCK_SCALA = "Mock.scala"
  }
}
