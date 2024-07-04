package com.jetbrains.edu.learning.codeforces.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.runConfigurationType
import com.jetbrains.edu.EducationalCoreIcons
import org.jetbrains.annotations.NonNls
import javax.swing.Icon

class CodeforcesRunConfigurationType : ConfigurationType {
  override fun getId(): String = CONFIGURATION_ID

  override fun getIcon(): Icon = EducationalCoreIcons.Codeforces

  override fun getConfigurationTypeDescription(): String = CONFIGURATION_ID

  override fun getDisplayName(): String = CONFIGURATION_ID

  override fun getConfigurationFactories(): Array<ConfigurationFactory> {
    return arrayOf(CodeforcesRunConfigurationFactory(this))
  }

  companion object {
    @NonNls
    const val CONFIGURATION_ID = "Codeforces"

    fun getInstance(): CodeforcesRunConfigurationType {
      return runConfigurationType()
    }
  }
}
