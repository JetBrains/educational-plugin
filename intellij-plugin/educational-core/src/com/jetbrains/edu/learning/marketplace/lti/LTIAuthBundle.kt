package com.jetbrains.edu.learning.marketplace.lti

import com.jetbrains.edu.learning.messages.EduPropertiesBundle
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "lti.lti-auth"

object LTIAuthBundle : EduPropertiesBundle(BUNDLE) {
  fun value(@PropertyKey(resourceBundle = BUNDLE) key: String): String {
    return valueOrEmpty(key)
  }
}