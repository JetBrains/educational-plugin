package com.jetbrains.edu.ai.provider

import com.intellij.openapi.project.Project
import com.jetbrains.edu.ai.translation.settings.TranslationProjectSettings
import com.jetbrains.edu.learning.ai.AIFeaturesProvider

class AIProvider : AIFeaturesProvider {
  override fun getLanguageTranslationCode(project: Project): String? =
    TranslationProjectSettings.getInstance(project).translatedToLanguage?.code
}