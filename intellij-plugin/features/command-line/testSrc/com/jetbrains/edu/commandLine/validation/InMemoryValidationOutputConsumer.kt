package com.jetbrains.edu.commandLine.validation

class InMemoryValidationOutputConsumer : ValidationOutputConsumer {

  private val validationOutput = StringBuilder()

  override fun consume(output: String) {
    validationOutput.appendLine(output)
  }

  fun output(): String = validationOutput.toString()
}
