package com.jetbrains.edu.cpp.checker

import com.intellij.execution.configurations.ConfigurationFactory
import com.jetbrains.cidr.execution.testing.google.CidrGoogleTestRunConfigurationType
import com.jetbrains.cidr.execution.testing.tcatch.CidrCatchTestRunConfigurationType

// BACKCOMPAT 2025.3. Inline it.
fun getGoogleTestConfigurationFactory(): ConfigurationFactory {
  return CidrGoogleTestRunConfigurationType.getInstance().getFactory()
}

// BACKCOMPAT 2025.3. Inline it.
fun getCatchTestConfigurationFactory(): ConfigurationFactory {
  return CidrCatchTestRunConfigurationType.getInstance().getFactory()
}