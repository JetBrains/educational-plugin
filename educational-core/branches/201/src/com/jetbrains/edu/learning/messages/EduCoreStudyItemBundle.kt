package com.jetbrains.edu.learning.messages

import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

@NonNls
private const val BUNDLE = "messages.EduCoreStudyItemBundle"

object EduCoreStudyItemBundle : EduBundle(BUNDLE) {
  @JvmStatic
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
    return getMessage(key, *params)
  }

  @JvmStatic
  fun lazyMessage(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): Supplier<String> {
    return Supplier { getMessage(key, *params) }
  }
}