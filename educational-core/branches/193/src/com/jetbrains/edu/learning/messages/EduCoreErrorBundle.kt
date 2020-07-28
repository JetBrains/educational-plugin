package com.jetbrains.edu.learning.messages

import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.EduCoreErrorBundle"

object EduCoreErrorBundle : EduBundle(BUNDLE) {
  @JvmStatic
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
    return getMessage(key, *params)
  }

  @JvmStatic
  fun lazyMessage(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
    // On platform 193 lazy messages not supported
    return getMessage(key, *params)
  }
}