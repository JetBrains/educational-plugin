package com.jetbrains.edu.go

import com.goide.GoIcons
import com.jetbrains.edu.go.checker.GoEduTaskChecker
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfiguratorWithSubmissions
import javax.swing.Icon

class GoConfigurator : EduConfiguratorWithSubmissions<GoProjectSettings>() {
  private val courseBuilder = GoCourseBuilder()

  override fun getCourseBuilder(): EduCourseBuilder<GoProjectSettings> = courseBuilder

  override fun getTestFileName() = ""

  override fun getMockFileName(text: String): String = "task.go"

  override fun getTestDirs() = listOf("test")

  override fun getTaskCheckerProvider(): TaskCheckerProvider = TaskCheckerProvider { task, project -> GoEduTaskChecker(project, task) }

  override fun getLogo(): Icon = GoIcons.ICON
}
