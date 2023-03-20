package com.jetbrains.edu.cpp.checker

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.openapi.options.advanced.AdvancedSettings
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.CidrTargetRunConfigurationProducer


private const val EMULATE_TERMINAL_SETTING_ID = "cidr.execution.emulate.terminal.in.output.console"

fun findOrCreateConfigurationFromContext(project: Project, context: ConfigurationContext): ConfigurationFromContext? {
  return CidrTargetRunConfigurationProducer.getInstances(project).firstOrNull()?.findOrCreateConfigurationFromContext(context)
}

fun <T> withoutTerminalEmulation(action: () -> T): T {
  val emulateTerminal = AdvancedSettings.getBoolean(EMULATE_TERMINAL_SETTING_ID)
  try {
    AdvancedSettings.setBoolean(EMULATE_TERMINAL_SETTING_ID, false)
    return action()
  }
  finally {
    AdvancedSettings.setBoolean(EMULATE_TERMINAL_SETTING_ID, emulateTerminal)
  }
}
