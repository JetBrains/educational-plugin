package com.jetbrains.edu.ai.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider

class EduAIConfigurableProvider : ConfigurableProvider(), Configurable.Beta {
  override fun createConfigurable(): Configurable = EduAIConfigurable()
}