package com.jetbrains.edu.learning.serialization

import com.intellij.util.xmlb.Accessor
import com.intellij.util.xmlb.SerializationFilter

class CompositeSerializationFilter(private val filters: Array<out SerializationFilter>) : SerializationFilter {
  override fun accepts(accessor: Accessor, bean: Any): Boolean {
    return filters.all { it.accepts(accessor, bean) }
  }
}

fun CompositeSerializationFilter(vararg filters: SerializationFilter): CompositeSerializationFilter = CompositeSerializationFilter(filters)
