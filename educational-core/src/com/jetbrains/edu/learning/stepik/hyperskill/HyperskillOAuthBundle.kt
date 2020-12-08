package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.messages.EduPropertiesBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE_NAME = "hyperskill.hyperskill-oauth"

object HyperskillOAuthBundle : EduPropertiesBundle(BUNDLE_NAME) {
  fun value(@PropertyKey(resourceBundle = BUNDLE_NAME) key: String): String {
    return valueOrEmpty(key)
  }
}
