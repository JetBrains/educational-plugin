package com.jetbrains.edu.decomposition.messages

import com.jetbrains.edu.learning.messages.EduBundle
import org.jetbrains.annotations.PropertyKey
import org.jetbrains.annotations.NonNls

@NonNls
private const val BUNDLE = "messages.EduDecompositionBundle"

object EduDecompositionBundle : EduBundle(BUNDLE) {
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = getMessage(key, *params)
}