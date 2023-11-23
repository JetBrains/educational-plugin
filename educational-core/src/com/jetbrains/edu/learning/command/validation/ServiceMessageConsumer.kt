package com.jetbrains.edu.learning.command.validation

import com.intellij.execution.testframework.sm.ServiceMessageBuilder

interface ServiceMessageConsumer {
  fun consume(message: ServiceMessageBuilder)
}

object StdoutServiceMessageConsumer : ServiceMessageConsumer {
  override fun consume(message: ServiceMessageBuilder) {
    println(message)
  }
}
