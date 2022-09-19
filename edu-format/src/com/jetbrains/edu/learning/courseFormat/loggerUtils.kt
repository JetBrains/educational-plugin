package com.jetbrains.edu.learning.courseFormat

import java.util.logging.Logger


internal inline fun <reified T : Any> logger(): Logger {
  return Logger.getLogger("#" + T::class.java.name)
}
