package com.jetbrains.edu.learning.messages

import com.intellij.CommonBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

object EduCoreBundle : EduBundle() {
  const val FAILED_TO_CONVERT_TO_STUDENT_FILE = "Failed to convert answer file to student one because placeholder is broken."

  @JvmStatic
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
    return CommonBundle.message(getBundle(BUNDLE), key, *params)
  }

  @NonNls
  private const val BUNDLE = "messages.EduCore"
}