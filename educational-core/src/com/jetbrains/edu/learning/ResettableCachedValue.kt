package com.jetbrains.edu.learning

import kotlin.reflect.KProperty

class ResettableCachedValue<T>(private val supplier: () -> T) {
  private var value: T? = null

  @Synchronized
  operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
      var currentValue = value
      if (currentValue != null) return currentValue
      currentValue = supplier()
      value = currentValue
      return currentValue
  }

  @Synchronized
  fun reset() {
    value = null
  }
}
