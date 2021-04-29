package com.jetbrains.edu.learning.encrypt

import com.jetbrains.edu.learning.messages.EduPropertiesBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "aes.aes"

object EncryptionBundle : EduPropertiesBundle(BUNDLE) {
  fun value(@PropertyKey(resourceBundle = BUNDLE) key: String): String {
    return valueOrEmpty(key)
  }
}