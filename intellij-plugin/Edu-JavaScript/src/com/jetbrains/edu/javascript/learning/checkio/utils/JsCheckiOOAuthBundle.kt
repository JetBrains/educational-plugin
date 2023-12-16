package com.jetbrains.edu.javascript.learning.checkio.utils

import com.jetbrains.edu.learning.messages.EduPropertiesBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE_NAME = "checkio.js-checkio-oauth"

object JsCheckiOOAuthBundle : EduPropertiesBundle(BUNDLE_NAME) {
  fun value(@PropertyKey(resourceBundle = BUNDLE_NAME) key:  String): String {
    return valueOrEmpty(key)
  }
}
