package com.jetbrains.edu.cpp.checker

import com.intellij.openapi.options.advanced.AdvancedSettings


private const val EMULATE_TERMINAL_SETTING_ID = "cidr.execution.emulate.terminal.in.output.console"

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
