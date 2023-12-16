package com.jetbrains.edu.cpp.codeforces

import com.intellij.execution.InputRedirectAware
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfigurationType
import com.jetbrains.cidr.cpp.execution.CMakeRunConfigurationType
import com.jetbrains.cidr.execution.BuildTargetAndConfigurationData
import com.jetbrains.cidr.execution.BuildTargetData
import com.jetbrains.cidr.execution.ExecutableData
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfigurationType
import com.jetbrains.edu.learning.getTaskFile

class CppCodeforcesRunConfiguration(project: Project, factory: ConfigurationFactory) :
  CMakeAppRunConfiguration(project, factory, CodeforcesRunConfigurationType.CONFIGURATION_ID),
  CodeforcesRunConfiguration {
  override fun setExecutableFile(file: VirtualFile) {
    val buildTargetData = BuildTargetData(project.name, getTargetName(file))
    executableData = ExecutableData(buildTargetData)
    targetAndConfigurationData = BuildTargetAndConfigurationData(buildTargetData, null)
  }

  private fun getTargetName(file: VirtualFile): String {
    val taskFile = file.getTaskFile(project) ?: throw IllegalStateException("Unable to find taskFile for virtual file " + file.path)
    return taskFile.task.name + "-run"
  }

  override fun getInputRedirectOptions(): InputRedirectAware.InputRedirectOptions = this

  override fun getType(): CMakeRunConfigurationType = CMakeAppRunConfigurationType()
}
