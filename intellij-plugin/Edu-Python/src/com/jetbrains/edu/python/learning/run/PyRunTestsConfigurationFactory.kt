package com.jetbrains.edu.python.learning.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class PyRunTestsConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration = PyRunTestConfiguration(project, this)

  override fun getId(): String = "Run Study Tests"
}
