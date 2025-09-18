package com.jetbrains.edu.commandLine.validation

class StdoutValidationOutputConsumer : ValidationOutputConsumer {
  override fun consume(output: String) {
    println(output)
  }

  override fun close() {}
}
