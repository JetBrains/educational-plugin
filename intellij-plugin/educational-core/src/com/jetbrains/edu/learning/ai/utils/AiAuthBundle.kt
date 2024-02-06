package com.jetbrains.edu.learning.ai.utils

import com.jetbrains.edu.learning.messages.EduPropertiesBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE_NAME = "ai.ai"

@Suppress("unused")
object AiAuthBundle : EduPropertiesBundle(BUNDLE_NAME) {
  private fun value(@Suppress("SameParameterValue") @PropertyKey(resourceBundle = BUNDLE_NAME) key: String): String {
    return valueOrEmpty(key)
  }

  /**
   * This is grazie temporary token used by ML4SE team
   * TODO remove it after user testing experiment
   */
  @Suppress("unused")
  fun getGrazieTemporaryToken(): String = value("grazieTemporaryToken")
}
