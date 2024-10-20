package com.jetbrains.edu.learning.ai

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.isUnitTestMode

interface AIFeaturesProvider {
  fun getLanguageTranslationCode(project: Project): String?

  companion object {
    private val EP_NAME = ExtensionPointName.create<AIFeaturesProvider>("Educational.AIFeaturesProvider")

    private fun getAIProvider(): AIFeaturesProvider? {
      val providers = EP_NAME.extensionList
      if (providers.size == 1) return providers[0]
      if (!isUnitTestMode) {
        if (providers.isEmpty()) {
          thisLogger().error("No AIFeaturesProvider available")
        }
        else {
          thisLogger().error("More than one AIFeaturesProvider available: $providers")
        }
      }
      return null
    }

    fun getTranslatedToLanguageCode(project: Project): String? = getAIProvider()?.getLanguageTranslationCode(project)
  }
}