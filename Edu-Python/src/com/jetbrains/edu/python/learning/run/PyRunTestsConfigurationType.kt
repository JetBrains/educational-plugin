package com.jetbrains.edu.python.learning.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.icons.AllIcons
import com.jetbrains.edu.python.learning.messages.EduPythonBundle.message
import javax.swing.Icon

class PyRunTestsConfigurationType : ConfigurationType {
  override fun getDisplayName(): String = message("tests.study.run")

  override fun getConfigurationTypeDescription(): String = message("tests.study.runner")

  override fun getIcon(): Icon = AllIcons.Actions.Lightning

  override fun getId(): String = "ccruntests"

  override fun getConfigurationFactories(): Array<ConfigurationFactory> {
    return arrayOf(PyRunTestsConfigurationFactory(this))
  }
}
