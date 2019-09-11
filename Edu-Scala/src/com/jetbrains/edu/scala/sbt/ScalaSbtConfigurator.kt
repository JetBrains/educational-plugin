package com.jetbrains.edu.scala.sbt

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfiguratorWithSubmissions
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import icons.EducationalCoreIcons
import javax.swing.Icon

class ScalaSbtConfigurator : EduConfiguratorWithSubmissions<JdkProjectSettings>() {

  private val myCourseBuilder = ScalaSbtCourseBuilder()

  override fun getCourseBuilder() = myCourseBuilder

  override fun getTestFileName(): String = TEST_SCALA

  override fun isEnabled(): Boolean = !EduUtils.isAndroidStudio()

  override fun getTaskCheckerProvider(): TaskCheckerProvider = TaskCheckerProvider { task, project -> ScalaSbtEduTaskChecker(task, project) }

  override fun getMockTemplate(): String = getInternalTemplateText(MOCK_SCALA)

  override fun getSourceDir(): String = EduNames.SRC

  override fun getTestDirs(): List<String> = listOf(EduNames.TEST)

  override fun getLogo(): Icon = EducationalCoreIcons.ScalaLogo

  override fun excludeFromArchive(project: Project, file: VirtualFile): Boolean {
    if (super.excludeFromArchive(project, file)) return true
    return generateSequence(file, VirtualFile::getParent).any { it.name == "target" || it.name == "project"}
  }

  companion object {
    const val TEST_SCALA = "TestSpec.scala"
    const val TASK_SCALA = "Task.scala"
    const val MOCK_SCALA = "Mock.scala"
  }
}
