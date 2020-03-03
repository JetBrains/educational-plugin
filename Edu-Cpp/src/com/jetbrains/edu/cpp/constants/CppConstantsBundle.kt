package com.jetbrains.edu.cpp.constants

import com.intellij.AbstractBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "constants.CppConstantsBundle"

object CppConstantsBundle : AbstractBundle(BUNDLE) {
  fun getConstant(@PropertyKey(resourceBundle = BUNDLE) key: String): String {
    return getMessage(key)
  }
}