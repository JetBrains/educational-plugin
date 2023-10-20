package com.jetbrains.edu.learning.courseFormat

import java.util.logging.Logger


internal inline fun <reified T : Any> logger(): Logger {
  return Logger.getLogger("#" + T::class.java.name)
}

internal fun logger(name: String): Logger {
  return Logger.getLogger("#$name")
}
