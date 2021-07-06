package com.jetbrains.edu.learning.codeforces.run

import com.intellij.execution.Executor
import com.intellij.execution.InputRedirectAware
import com.intellij.execution.configuration.AbstractRunConfiguration
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.NonNls

// This is a hack for ignoring Codeforces configuration if we have issues to set it up properly.
class InvalidCodeforcesRunConfiguration(project: Project, factory: ConfigurationFactory) :
  CodeforcesRunConfiguration, AbstractRunConfiguration(project, factory) {
  @NonNls
  private val message = "This code should be unreachable"

  override fun setExecutableFile(file: VirtualFile) = error(message)
  override fun getInputRedirectOptions(): InputRedirectAware.InputRedirectOptions = error(message)
  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = error(message)
  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState = error(message)
  override fun getValidModules(): MutableCollection<Module> = error(message)
}
