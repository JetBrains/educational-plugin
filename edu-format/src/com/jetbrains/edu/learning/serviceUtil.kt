package com.jetbrains.edu.learning

import java.util.*

internal fun <S> findService(service: Class<S>): S {
  //https://plugins.jetbrains.com/docs/intellij/plugin-class-loaders.html#using-serviceloader

  val currentThread = Thread.currentThread()
  val originalClassLoader = currentThread.getContextClassLoader()
  val pluginClassLoader = object{}.javaClass.enclosingClass.getClassLoader()
  try {
    currentThread.setContextClassLoader(pluginClassLoader)
    val serviceLoader = ServiceLoader.load(service)
    return serviceLoader.findFirst().get()
  }
  finally {
    currentThread.setContextClassLoader(originalClassLoader)
  }
}