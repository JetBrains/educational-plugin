package com.jetbrains.edu.learning

import kotlin.reflect.KProperty

fun <T> threadLocal(initializer: () -> T): ThreadLocalDelegate<T> = ThreadLocalDelegate(initializer)

class ThreadLocalDelegate<T>(initializer: () -> T) {
  private val tl: ThreadLocal<T> = ThreadLocal.withInitial(initializer)

  operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
    return tl.get()
  }

  operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    tl.set(value)
  }
}
