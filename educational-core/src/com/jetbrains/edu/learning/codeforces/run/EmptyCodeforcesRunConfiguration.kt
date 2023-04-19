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
import com.intellij.ui.components.JBPanelWithEmptyText
import javax.swing.JComponent

// This is a hack for ignoring Codeforces configuration if we have issues to set it up properly.
// Just shows empty config and does nothing on run
class EmptyCodeforcesRunConfiguration(project: Project, factory: ConfigurationFactory) :
  CodeforcesRunConfiguration, AbstractRunConfiguration(project, factory) {

  override fun getInputRedirectOptions(): InputRedirectAware.InputRedirectOptions = EmptyRedirectOutput()
  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = EmptySettingsEditor()
  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? = null
  override fun getValidModules(): Collection<Module>? = null
}

private class EmptyRedirectOutput : InputRedirectAware.InputRedirectOptions {
  override fun isRedirectInput(): Boolean = false
  override fun setRedirectInput(value: Boolean) {}
  override fun getRedirectInputPath(): String? = null
  override fun setRedirectInputPath(value: String?) {}
}

private class EmptySettingsEditor : SettingsEditor<RunConfiguration>() {
  override fun resetEditorFrom(s: RunConfiguration) {}
  override fun applyEditorTo(s: RunConfiguration) {}
  override fun createEditor(): JComponent = JBPanelWithEmptyText()
}
