package com.jetbrains.edu.learning.courseFormat

import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal inline fun <reified T : Any> logger(): Logger {
  return LoggerFactory.getLogger("#" + T::class.java.name)
}
