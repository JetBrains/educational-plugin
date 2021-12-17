package com.jetbrains.edu.php

import com.intellij.openapi.extensions.PluginId
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import javax.swing.Icon

class PhpConfigurator : EduConfigurator<PhpProjectSettings> {

  override val courseBuilder: EduCourseBuilder<PhpProjectSettings>
    get() = PhpCourseBuilder()

  override val testFileName: String
    get() = TEST_PHP

  override val taskCheckerProvider: TaskCheckerProvider
    get() = PhpTaskCheckerProvider()

  override val logo: Icon
    get() = EducationalCoreIcons.PhpLogo

  override val isEnabled: Boolean
    get() = isFeatureEnabled(EduExperimentalFeatures.PHP_COURSES)

  override val testDirs: List<String>
    get() = listOf(EduNames.TEST)

  override val sourceDir: String
    get() = EduNames.SRC

  override val pluginRequirements: List<PluginId>
    get() = listOf(PluginId.getId("com.jetbrains.php"))

  override fun getMockFileName(text: String): String = TASK_PHP

  override fun isTestFile(task: Task, path: String): Boolean = super.isTestFile(task, path) || path == testFileName

  companion object {
    const val MAIN_PHP = "main.php"
    const val TASK_PHP = "task.php"
    const val TEST_PHP = "test.php"
  }
}