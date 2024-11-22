package com.jetbrains.edu.learning.socialMedia.x

import com.jetbrains.edu.learning.messages.EduPropertiesBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "twitter.oauth_twitter"

object XBundle : EduPropertiesBundle(BUNDLE) {
  fun value(@PropertyKey(resourceBundle = BUNDLE) key: String): String {
    return valueOrEmpty(key)
  }
}
