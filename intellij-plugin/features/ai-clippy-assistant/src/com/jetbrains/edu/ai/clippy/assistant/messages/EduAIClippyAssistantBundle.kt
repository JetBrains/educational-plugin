package com.jetbrains.edu.ai.clippy.assistant.messages

import com.jetbrains.edu.learning.messages.EduBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

@NonNls
private const val BUNDLE = "messages.EduAIClippyAssistantBundle"

object EduAIClippyAssistantBundle : EduBundle(BUNDLE) {
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String = getMessage(key, *params)

  fun lazyMessage(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): Supplier<String> =
    Supplier { getMessage(key, *params) }
}