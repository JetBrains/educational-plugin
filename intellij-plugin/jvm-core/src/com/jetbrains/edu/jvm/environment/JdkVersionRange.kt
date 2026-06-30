package com.jetbrains.edu.jvm.environment

import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.jetbrains.edu.jvm.messages.EduJVMBundle

/**
 * nulls mean that the range is not bound from the corresponding side
 */
data class JdkVersionRange(val min: Int?, val max: Int?) {

  fun intersect(other: JdkVersionRange): JdkVersionRange {
    val newMin = when {
      min == null -> other.min
      other.min == null -> min
      else -> maxOf(min, other.min)
    }
    val newMax = when {
      max == null -> other.max
      other.max == null -> max
      else -> minOf(max, other.max)
    }
    return JdkVersionRange(newMin, newMax)
  }

  fun nearestVersion(version: Int): Int {
    return version.coerceIn(min, max)
  }

  fun contains(version: Int): Boolean {
    return (min == null || version >= min) && (max == null || version <= max)
  }

  fun contains(version: JavaSdkVersion): Boolean {
    return contains(version.maxLanguageLevel.feature())
  }

  fun isEmpty(): Boolean = min != null && max != null && min > max

  fun userString(): String {
    return if (min == null) {
      if (max == null) EduJVMBundle.message("range.all") else EduJVMBundle.message("range.up.to", max)
    }
    else {
      if (max == null) EduJVMBundle.message("range.from", min) else EduJVMBundle.message("range.from.to", min, max)
    }
  }

  companion object {
    val All = JdkVersionRange(null, null)
  }
}