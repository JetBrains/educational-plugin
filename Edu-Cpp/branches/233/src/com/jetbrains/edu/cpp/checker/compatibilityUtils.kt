package com.jetbrains.edu.cpp.checker


fun <T> withoutTerminalEmulation(action: () -> T): T {
  return action()
}
