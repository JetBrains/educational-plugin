package com.jetbrains.edu.learning

import java.util.*

internal inline fun <reified S> findService(): S =
  findServiceOrNull<S>() ?: error("No implementation found for service ${S::class.java.name}")

/**
 * Same as [findService], but returns `null` instead of throwing when no implementation is registered.
 * Useful when [edu-format] is used standalone (outside the plugin) and the service may be absent.
 */
internal inline fun <reified S> findServiceOrNull(): S? {
  //https://plugins.jetbrains.com/docs/intellij/plugin-class-loaders.html#using-serviceloader

  val currentThread = Thread.currentThread()
  val originalClassLoader = currentThread.getContextClassLoader()
  val pluginClassLoader = object{}.javaClass.enclosingClass.getClassLoader()
  try {
    currentThread.setContextClassLoader(pluginClassLoader)
    val serviceLoader = ServiceLoader.load(S::class.java)
    return serviceLoader.findFirst().orElse(null)
  }
  finally {
    currentThread.setContextClassLoader(originalClassLoader)
  }
}