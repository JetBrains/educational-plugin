package com.jetbrains.edu.scala.sbt.checker

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.checker.tests.TestResultCollector
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestDirectories
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestConfigurationType
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestRunConfiguration
import org.jetbrains.plugins.scala.testingSupport.test.testdata.AllInPackageTestData

class ScalaSbtEduTaskChecker(
  task: EduTask,
  envChecker: EnvironmentChecker,
  project: Project
) : EduTaskCheckerBase(task, envChecker, project) {
  override fun createDefaultTestConfigurations(): List<RunnerAndConfigurationSettings> {
    val configurations = createTestConfigurationsForTestDirectories().filter { it.configuration.type == preferredConfigurationType }
    return if (configurations.isEmpty()) {
      task.getAllTestDirectories(project).map { createConfiguration(it) }
    }
    else {
      configurations
    }
  }

  @Suppress("UnstableApiUsage")
  private fun createConfiguration(testDirectory: PsiDirectory): RunnerAndConfigurationSettings {
    val settings = RunManager.getInstance(project).createConfiguration("Scala tests", ScalaTestConfigurationType().confFactory())
    val configuration = settings.configuration as ScalaTestRunConfiguration
    val packageTestData = AllInPackageTestData(configuration)
    packageTestData.workingDirectory = testDirectory.project.basePath
    configuration.`testConfigurationData_$eq`(packageTestData)
    configuration.testKind = packageTestData.kind
    configuration.module = ModuleUtilCore.findModuleForPsiElement(testDirectory)
    return settings
  }

  override fun createTestResultCollector(): TestResultCollector = ScalaTestResultCollector()
}
