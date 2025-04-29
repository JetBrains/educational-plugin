package com.jetbrains.edu.ai.debugger.core.log

import com.intellij.idea.LoggerFactory
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.lazyPub

object Logger {
  private val default: Logger = LoggerFactory().getLoggerInstance("ai-debugging")

  val aiDebuggingLogger: Logger by lazyPub {
    AIDebuggingLoggerFactory.getLoggerInstanceOrNull() ?: default
  }
}