package com.jetbrains.edu.go

import com.goide.GoIcons
import com.jetbrains.edu.go.checker.GoEduTaskChecker
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.EduNames.TEST
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfiguratorWithSubmissions
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.isUnitTestMode
import javax.swing.Icon

class GoConfigurator : EduConfiguratorWithSubmissions<GoProjectSettings>() {
  private val courseBuilder = GoCourseBuilder()

  override fun getCourseBuilder(): EduCourseBuilder<GoProjectSettings> = courseBuilder

  override fun getTestFileName() = TEST_GO

  override fun getMockFileName(text: String): String = TASK_GO

  override fun getTestDirs() = listOf(TEST)

  override fun getTaskCheckerProvider(): TaskCheckerProvider = TaskCheckerProvider { task, project -> GoEduTaskChecker(project, task) }

  override fun getLogo(): Icon = GoIcons.ICON

  override fun isEnabled(): Boolean = isFeatureEnabled(EduExperimentalFeatures.GO_COURSES) || isUnitTestMode

  companion object {
    const val TEST_GO = "task_test.go"
    const val TASK_GO = "task.go"
    const val MAIN_GO = "main.go"
  }
}
