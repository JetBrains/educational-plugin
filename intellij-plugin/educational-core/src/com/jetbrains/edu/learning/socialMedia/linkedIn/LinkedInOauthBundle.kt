package com.jetbrains.edu.learning.socialMedia.linkedIn

import com.jetbrains.edu.learning.messages.EduPropertiesBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE_NAME = "linkedin.linkedin-oauth"

object LinkedInOAuthBundle : EduPropertiesBundle(BUNDLE_NAME) {
  fun value(@PropertyKey(resourceBundle = BUNDLE_NAME) key: String): String {
    return valueOrEmpty(key)
  }
}
