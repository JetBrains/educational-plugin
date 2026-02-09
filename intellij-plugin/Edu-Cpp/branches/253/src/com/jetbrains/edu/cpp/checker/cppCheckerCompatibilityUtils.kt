package com.jetbrains.edu.cpp.checker

import com.intellij.execution.configurations.ConfigurationFactory
import com.jetbrains.cidr.cpp.execution.testing.google.CMakeGoogleTestRunConfigurationType
import com.jetbrains.cidr.cpp.execution.testing.tcatch.CMakeCatchTestRunConfigurationType

fun getGoogleTestConfigurationFactory(): ConfigurationFactory {
  return CMakeGoogleTestRunConfigurationType.getInstance().factory
}

fun getCatchTestConfigurationFactory(): ConfigurationFactory {
  return CMakeCatchTestRunConfigurationType.getInstance().factory
}