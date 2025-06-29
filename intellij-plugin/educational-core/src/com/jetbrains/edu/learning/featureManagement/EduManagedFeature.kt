package com.jetbrains.edu.learning.featureManagement


enum class EduManagedFeature(val featureKey: String) {
  AI_HINTS("ai-hints");

  companion object {
    fun forKey(featureKey: String): EduManagedFeature? = values().find { it.featureKey == featureKey }
  }
}