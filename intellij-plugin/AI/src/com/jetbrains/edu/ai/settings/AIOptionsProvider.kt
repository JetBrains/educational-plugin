package com.jetbrains.edu.ai.settings

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableEP

interface AIOptionsProvider : Configurable {
  fun isAvailable(): Boolean = true

  companion object {
    val EP_NAME: ExtensionPointName<ConfigurableEP<AIOptionsProvider>> = ExtensionPointName.create("Educational.AIOptionsProvider")
  }
}