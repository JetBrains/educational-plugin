package com.jetbrains.edu.learning.socialmedia.twitter

import com.jetbrains.edu.learning.messages.EduPropertiesBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "twitter.oauth_twitter"

object TwitterBundle : EduPropertiesBundle(BUNDLE) {
  fun value(@PropertyKey(resourceBundle = BUNDLE) key: String): String {
    return valueOrEmpty(key)
  }
}
