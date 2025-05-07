package com.jetbrains.edu.ai.debugger.core.messages

import com.jetbrains.edu.learning.messages.EduBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE: String = "messages.EduAIDebuggerCoreBundle"

object EduAIDebuggerCoreBundle : EduBundle(BUNDLE) {
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String = getMessage(key, *params)
}