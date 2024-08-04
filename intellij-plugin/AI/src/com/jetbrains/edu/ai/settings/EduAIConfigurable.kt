package com.jetbrains.edu.ai.settings

import com.intellij.openapi.extensions.BaseExtensionPointName
import com.intellij.openapi.options.CompositeConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ex.ConfigurableWrapper
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.edu.ai.messages.EduAIBundle
import javax.swing.JComponent

class EduAIConfigurable : CompositeConfigurable<AIOptionsProvider>(), Configurable.Beta, Configurable.WithEpDependencies {
  override fun createComponent(): JComponent =
    panel {
      for (configurable in configurables) {
        val component = configurable.createComponent() ?: continue

        row {
          cell(component)
            .align(AlignX.FILL)
        }
      }
    }.apply {
      layout = VerticalFlowLayout()
    }

  override fun getDisplayName(): String = EduAIBundle.message("settings.ai")

  override fun createConfigurables(): List<AIOptionsProvider> =
    ConfigurableWrapper.createConfigurables(AIOptionsProvider.EP_NAME).filter { it.isAvailable() }

  override fun getDependencies(): Collection<BaseExtensionPointName<*>> = listOf(AIOptionsProvider.EP_NAME)

  companion object {
    @Suppress("unused")
    const val ID: String = "com.jetbrains.edu.ai.EduAIConfigurable"
  }
}