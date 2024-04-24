package com.jetbrains.edu.cognifire.log

import com.intellij.idea.LoggerFactory
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.lazyPub

object Logger {
  private val default: Logger = LoggerFactory().getLoggerInstance("Cognifire")

  val cognifireLogger: Logger by lazyPub {
    CognifireLoggerFactory().getLoggerInstanceOrNull() ?: default
  }

  val cognifireStudyLogger: Logger by lazyPub {
    CognifireStudyLoggerFactory().getLoggerInstanceOrNull() ?: default
  }
}
