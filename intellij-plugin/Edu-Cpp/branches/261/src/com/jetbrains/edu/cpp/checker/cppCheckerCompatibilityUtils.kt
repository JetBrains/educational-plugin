package com.jetbrains.edu.cpp.checker

import com.intellij.execution.configurations.ConfigurationFactory
import com.jetbrains.cidr.execution.testing.google.CidrGoogleTestRunConfigurationType
import com.jetbrains.cidr.execution.testing.tcatch.CidrCatchTestRunConfigurationType

fun getGoogleTestConfigurationFactory(): ConfigurationFactory {
  return CidrGoogleTestRunConfigurationType.getInstance().getFactory()
}

fun getCatchTestConfigurationFactory(): ConfigurationFactory {
  return CidrCatchTestRunConfigurationType.getInstance().getFactory()
}