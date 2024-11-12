package com.jetbrains.edu.ai.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.isFeatureEnabled

class EduAIConfigurableProvider : ConfigurableProvider(), Configurable.Beta {
  override fun createConfigurable(): Configurable = EduAIConfigurable()

  override fun canCreateConfigurable(): Boolean = isFeatureEnabled(EduExperimentalFeatures.AI)
}