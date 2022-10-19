@file:JvmName("TaskUtils")
package com.jetbrains.edu.learning.taskDescription.ui

fun loadText(filePath: String): String? {
  val stream = try {
    object {}.javaClass.getResourceAsStream(filePath)
  } catch (e: NullPointerException) {
    return null
  }

  return stream.use {
    it.bufferedReader().readText()
  }
}
