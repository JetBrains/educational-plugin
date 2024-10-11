package com.jetbrains.edu.commandLine.validation

import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.jetbrains.edu.coursecreator.validation.ServiceMessageConsumer

object StdoutServiceMessageConsumer : ServiceMessageConsumer {
  override fun consume(message: ServiceMessageBuilder) {
    println(message)
  }
}
