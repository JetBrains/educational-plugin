package com.jetbrains.edu.cpp.checker

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.cidr.cpp.cmake.model.CMakeTarget
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import com.jetbrains.cidr.cpp.execution.testing.CMakeTestRunConfiguration
import com.jetbrains.cidr.execution.BuildTargetAndConfigurationData
import com.jetbrains.cidr.execution.BuildTargetData
import com.jetbrains.cidr.execution.ExecutableData
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestFiles
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

open class CppEduTaskChecker(task: EduTask, envChecker: EnvironmentChecker, project: Project) : EduTaskCheckerBase(task, envChecker, project) {
  override fun createDefaultTestConfigurations(): List<RunnerAndConfigurationSettings> {
    val configurationFromContext = createTestConfigurationsForTestFiles()
    return configurationFromContext.ifEmpty {
      val factory = getFactory() ?: return emptyList()

      val targets = getAllCmakeTargetsForTestFiles()
      targets.map {
        createConfiguration(it, factory)
      }
    }
  }

  private fun getAllCmakeTargetsForTestFiles(): List<CMakeTarget> {
    val model = CMakeWorkspace.getInstance(project).model ?: return emptyList()
    val allTestFiles = task.getAllTestFiles(project)

    return allTestFiles.mapNotNull { file ->
      model.targets.find { target ->
        target.buildConfigurations.find { config ->
          config.sources.map { FileUtil.toSystemIndependentName(it.path) }.contains(file.virtualFile.path)
        } != null
      }
    }
  }

  private fun createConfiguration(cMakeTarget: CMakeTarget, factory: ConfigurationFactory): RunnerAndConfigurationSettings {
    val configurationSettings = RunManager.getInstance(project).createConfiguration(cMakeTarget.name, factory)
    val cMakeTestRunConfiguration = configurationSettings.configuration as CMakeTestRunConfiguration
    val buildTargetData = BuildTargetData(cMakeTarget)
    cMakeTestRunConfiguration.targetAndConfigurationData = BuildTargetAndConfigurationData(buildTargetData, cMakeTarget.name)
    cMakeTestRunConfiguration.executableData = ExecutableData(buildTargetData)
    return configurationSettings
  }

  protected open fun getFactory(): ConfigurationFactory? = null
}

class CppCatchEduTaskChecker(task: EduTask, envChecker: EnvironmentChecker, project: Project) : CppEduTaskChecker(task, envChecker, project) {
  override fun getFactory(): ConfigurationFactory = getCatchTestConfigurationFactory()
}

class CppGEduTaskChecker(task: EduTask, envChecker: EnvironmentChecker, project: Project) : CppEduTaskChecker(task, envChecker, project) {
  override fun getFactory(): ConfigurationFactory = getGoogleTestConfigurationFactory()
}
