package com.jetbrains.edu.cpp.checker

import com.intellij.openapi.project.Project

fun <T> withoutTerminalEmulation(action: () -> T): T {
  return action()
}
