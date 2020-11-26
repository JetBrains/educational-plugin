package com.jetbrains.edu.learning.serialization

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * It's supposed to be used not to serialize `true` value when it's a default value via [JsonInclude] annotation API
 */
@Suppress("EqualsOrHashCode")
class TrueValueFilter {
  override fun equals(other: Any?): Boolean = other is Boolean && other
}
