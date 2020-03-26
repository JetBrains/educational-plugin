package com.jetbrains.edu.learning.messages

import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
const val EDU_CORE_BUNDLE_NAME = "messages.EduCoreBundle"

object EduCoreBundle : EduBundle(EDU_CORE_BUNDLE_NAME) {
  const val FAILED_TO_CONVERT_TO_STUDENT_FILE = "Failed to convert answer file to student one because placeholder is broken."

  @JvmStatic
  fun message(@PropertyKey(resourceBundle = EDU_CORE_BUNDLE_NAME) key: String, vararg params: Any): String {
    return getMessage(key, *params)
  }
}