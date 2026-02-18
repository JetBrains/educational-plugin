package com.jetbrains.edu.lti

import com.jetbrains.edu.learning.messages.EduBundle
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.LTIBundle"

object LTIBundle : EduBundle(BUNDLE) {
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
    return getMessage(key, *params)
  }
}