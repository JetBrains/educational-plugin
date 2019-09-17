package com.jetbrains.edu.cpp.messages

import com.intellij.CommonBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

object EduCppBundle {

  @JvmStatic
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
    return CommonBundle.message(BUNDLE, key, *params)
  }

  @NonNls
  private const val BUNDLE = "messages.EduCpp"
}