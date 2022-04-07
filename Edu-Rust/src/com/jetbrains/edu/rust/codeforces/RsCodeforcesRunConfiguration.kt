package com.jetbrains.edu.rust.codeforces

import com.intellij.execution.InputRedirectAware
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfigurationType
import org.rust.cargo.runconfig.command.CargoCommandConfiguration

class RsCodeforcesRunConfiguration(
  project: Project,
  factory: ConfigurationFactory
) : CargoCommandConfiguration(project, CodeforcesRunConfigurationType.CONFIGURATION_ID, factory),
    CodeforcesRunConfiguration {

  override fun getInputRedirectOptions(): InputRedirectAware.InputRedirectOptions {
    return this
  }
}