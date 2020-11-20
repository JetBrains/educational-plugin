package com.jetbrains.edu.learning.codeforces.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.runConfigurationType
import icons.EducationalCoreIcons
import org.jetbrains.annotations.NonNls

/**
 * id has to be one of these values
 * @see com.intellij.execution.InputRedirectAware.TYPES_WITH_REDIRECT_AWARE_UI
 * in other cases, Java configuration will not work
 */
class CodeforcesRunConfigurationType : ConfigurationTypeBase("Application", CONFIGURATION_ID, CONFIGURATION_ID,
                                                             EducationalCoreIcons.Codeforces) {
  override fun getDisplayName(): String {
    return CONFIGURATION_ID
  }

  override fun getConfigurationFactories(): Array<ConfigurationFactory> {
    return arrayOf(CodeforcesRunConfigurationFactory(this))
  }

  companion object {
    @NonNls
    const val CONFIGURATION_ID = "Codeforces Configuration"

    fun getInstance(): CodeforcesRunConfigurationType {
      return runConfigurationType()
    }
  }
}
