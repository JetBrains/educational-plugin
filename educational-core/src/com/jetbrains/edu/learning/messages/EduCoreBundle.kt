package com.jetbrains.edu.learning.messages

import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

@NonNls
const val BUNDLE = "messages.EduCoreBundle"

object EduCoreBundle : EduBundle(BUNDLE) {
  const val FAILED_TO_CONVERT_TO_STUDENT_FILE = "Failed to convert answer file to student one because placeholder is broken."

  @JvmStatic
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
    return getMessage(key, *params)
  }

  @JvmStatic
  fun lazyMessage(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): Supplier<String> {
    return Supplier { getMessage(key, *params) }
  }
}