package com.jetbrains.edu.ai.terms.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.settings.AIOptionsProvider
import com.jetbrains.edu.ai.translation.statistics.EduAIFeaturesCounterUsageCollector
import com.jetbrains.edu.learning.ai.terms.TheoryLookupProperties
import com.jetbrains.edu.learning.ai.terms.TheoryLookupSettings

class TheoryLookupOptions : BoundConfigurable(EduAIBundle.message("settings.ai.terms.display.name")), AIOptionsProvider {
  private var isEnabled: Boolean = TheoryLookupSettings.getInstance().isTheoryLookupEnabled

  override fun createPanel(): DialogPanel = panel {
    group(displayName) {
      row {
        checkBox(EduAIBundle.message("settings.ai.terms.is.enabled"))
          .bindSelected(::isEnabled)
      }
    }
  }

  override fun apply() {
    super.apply()
    val wasEnabled = TheoryLookupSettings.getInstance().isTheoryLookupEnabled
    if (wasEnabled && !isEnabled) {
      EduAIFeaturesCounterUsageCollector.theoryLookupDisabled()
    }
    val theoryLookupProperties = TheoryLookupProperties(isEnabled)
    TheoryLookupSettings.getInstance().setTheoryLookupProperties(theoryLookupProperties)
  }
}