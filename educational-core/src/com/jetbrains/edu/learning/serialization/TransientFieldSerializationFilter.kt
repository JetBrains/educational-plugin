package com.jetbrains.edu.learning.serialization

import com.intellij.serialization.PropertyAccessor
import com.intellij.util.xmlb.Accessor
import com.intellij.util.xmlb.SerializationFilter
import java.lang.reflect.Modifier

/**
 * Skips accesses to fields marked with `transient` keyword (in Java) or [Transient] annotation in Kotlin
 */
object TransientFieldSerializationFilter : SerializationFilter {

  override fun accepts(accessor: Accessor, bean: Any): Boolean {
    return !isTransientFieldAccess(accessor, bean)
  }

  @Suppress("UnstableApiUsage")
  private fun isTransientFieldAccess(accessor: Accessor, bean: Any): Boolean {
    if (accessor !is PropertyAccessor) return false
    val field = try {
      // Do we need some cache for reflection here?
      // If so, see `com.intellij.util.xmlb.XmlSerializerImpl.XmlSerializer` as an example
      bean.javaClass.getField(accessor.name)
    }
    catch (ignore: NoSuchFieldException) {
      return false
    }

    return Modifier.isTransient(field.modifiers)
  }
}
