package com.jetbrains.edu.scala.sbt

import com.intellij.build.BuildProgressListener
import com.intellij.build.BuildViewManager
import com.intellij.build.events.BuildEvent
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckResult.Companion.noTestsRun
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestDirectories
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.xmlEscaped
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

  override fun check(indicator: ProgressIndicator): CheckResult {
    val buildViewManager = project.getService(BuildViewManager::class.java)
    val buildEvents = mutableListOf<BuildEvent>()
    val listener = BuildProgressListener { _, event -> buildEvents.add(event) }

    @Suppress("UnstableApiUsage")
    buildViewManager.addListener(listener, buildViewManager)

    val checkResult = super.check(indicator)

    if (checkResult == noTestsRun && buildEvents.find { it.message.contains("Errors occurred") } != null) {
      val description = buildEvents.find { it.description != null }?.description
      return CheckResult(CheckStatus.Unchecked, CheckUtils.COMPILATION_FAILED_MESSAGE, description)
    }
    return checkResult
  }

  override fun getErrorMessage(node: SMTestProxy): String = super.getErrorMessage(node).xmlEscaped
}
