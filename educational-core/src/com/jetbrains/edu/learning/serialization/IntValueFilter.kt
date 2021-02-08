package com.jetbrains.edu.learning.serialization

/**
 * It's supposed to be used not to serialize `0` value when it's a default value via [JsonInclude] annotation API
 */
@Suppress("EqualsOrHashCode")
class IntValueFilter {
  private val defaultValue: Int = 0

  override fun equals(other: Any?): Boolean = other is Int && other == defaultValue
}