package com.jetbrains.edu.python.learning.checkio.utils

import com.jetbrains.edu.learning.messages.EduPropertiesBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE_NAME = "checkio.py-checkio-oauth"

object PyCheckiOOAuthBundle : EduPropertiesBundle(BUNDLE_NAME) {
  fun value(@PropertyKey(resourceBundle = BUNDLE_NAME) key: String): String {
    return valueOrEmpty(key)
  }
}
