package com.jetbrains.edu.cpp.checker

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.CidrTargetRunConfigurationProducer

fun findOrCreateConfigurationFromContext(project: Project, context: ConfigurationContext): ConfigurationFromContext? {
  return CidrTargetRunConfigurationProducer.getInstance(project)?.findOrCreateConfigurationFromContext(context)
}
