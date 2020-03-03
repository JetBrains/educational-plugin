@file:Suppress("DEPRECATION")

package com.jetbrains.edu.learning

import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher

inline fun <reified T> nullValue(): Matcher<T> = CoreMatchers.nullValue(T::class.java)

fun withFeature(featureId: String, enabled: Boolean, action: () -> Unit) {
  val currentValue = isFeatureEnabled(featureId)
  setFeatureEnabled(featureId, enabled)
  try {
    action()
  }
  finally {
    setFeatureEnabled(featureId, currentValue)
  }
}

