package com.jetbrains.edu.scala.sbt.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.checker.tests.TestResultCollector
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

class ScalaSbtEduTaskChecker(
  task: EduTask,
  envChecker: EnvironmentChecker,
  project: Project
) : EduTaskCheckerBase(task, envChecker, project) {
  override fun createDefaultTestConfigurations(): List<RunnerAndConfigurationSettings> {
    // IDE always suggests "Scala Test" configurations for directories, even if files inside use the "MUnit" framework

    val fileConfigurations = createTestConfigurationsForTestFiles()
    if (fileConfigurations.isNotEmpty()) {
      return fileConfigurations
    }

    val directoryConfigurations = createTestConfigurationsForTestDirectories()
    return directoryConfigurations
  }

  override fun createTestResultCollector(): TestResultCollector = ScalaTestResultCollector()
}
