package com.jetbrains.edu.learning.featureManagement


enum class EduManagedFeature(val featureKey: String) {
  AI_HINTS("ai-hints"),
  AI_COMPLETION("ai-completion");

  companion object {
    fun forKey(featureKey: String): EduManagedFeature? = entries.find { it.featureKey == featureKey }
  }
}