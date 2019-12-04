package com.jetbrains.edu.rust.slow.checker

import com.intellij.openapi.util.registry.Registry

private const val CARGO_BUILD_TOOL_WINDOW_KEY = "cargo.build.tool.window.enabled"

fun withCargoBuildToolWindow(action: () -> Unit) {
  val value = Registry.get(CARGO_BUILD_TOOL_WINDOW_KEY)
  val currentValue = value.asBoolean()
  try {
    value.setValue(true)
    action()
  }
  finally {
    value.setValue(currentValue)
  }
}
