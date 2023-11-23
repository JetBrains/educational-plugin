package com.jetbrains.edu.scala.sbt

import ch.epfl.scala.bsp4j.CompileResult
import com.intellij.compiler.CompilerWorkspaceConfiguration
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.compiler.CompilerMessageCategory
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.task.ProjectTaskManager
import com.intellij.task.impl.JpsProjectTaskRunner
import com.intellij.util.SystemProperties
import com.intellij.util.containers.stream
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckResult.Companion.noTestsRun
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestDirectories
import com.jetbrains.edu.learning.courseFormat.ext.shouldBeEmpty
import com.jetbrains.edu.learning.courseFormat.ext.shouldGenerateTestsOnTheFly
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.runReadActionInSmartMode
import com.jetbrains.edu.learning.xmlEscaped
import org.jetbrains.jps.api.GlobalOptions
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
    val checkResult = super.check(indicator)

    if (checkResult == noTestsRun) {
      val taskManager = ProjectTaskManager.getInstance(project)
      val compileDirectories =  runReadActionInSmartMode(project) {task.getAllTestDirectories(project).map { it.virtualFile }.toTypedArray()}

      val autoShowErrorsInEditorSelectedValue = CompilerWorkspaceConfiguration.getInstance(project).AUTO_SHOW_ERRORS_IN_EDITOR

      val doNotShowFileInEditor =  task.shouldGenerateTestsOnTheFly()
      if (doNotShowFileInEditor && !autoShowErrorsInEditorSelectedValue) {
        CompilerWorkspaceConfiguration.getInstance(project).AUTO_SHOW_ERRORS_IN_EDITOR = false
      }

      var description: String? = null
      taskManager.compile(*compileDirectories).onSuccess { result ->
        description = result?.context?.get()
          ?.get(JpsProjectTaskRunner.JPS_BUILD_DATA_KEY)
          ?.finishedBuildsContexts?.flatMap { compileContext ->
            compileContext.getMessages(CompilerMessageCategory.ERROR).stream().map { it.message }.toList()!!
          }
          ?.reduce { t, u -> "$t\n$u" }
      }.blockingGet(30_000)
      if (doNotShowFileInEditor && autoShowErrorsInEditorSelectedValue) {
        CompilerWorkspaceConfiguration.getInstance(project).AUTO_SHOW_ERRORS_IN_EDITOR = autoShowErrorsInEditorSelectedValue
      }

      description ?: return checkResult
      return CheckResult(CheckStatus.Failed, CheckUtils.COMPILATION_FAILED_MESSAGE, description)
    }
    return checkResult
  }

  override fun getErrorMessage(node: SMTestProxy): String = super.getErrorMessage(node).xmlEscaped
}
