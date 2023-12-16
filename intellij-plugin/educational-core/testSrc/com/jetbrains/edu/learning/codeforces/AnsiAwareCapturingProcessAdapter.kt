package com.jetbrains.edu.learning.codeforces

import com.intellij.execution.process.*
import com.intellij.openapi.util.Key

/**
 * Capturing adapter that removes ANSI escape codes from the output
 */
class AnsiAwareCapturingProcessAdapter : ProcessAdapter(), AnsiEscapeDecoder.ColoredTextAcceptor {
  val output = ProcessOutput()

  private val decoder = object : AnsiEscapeDecoder() {
    override fun getCurrentOutputAttributes(outputType: Key<*>) = outputType
  }

  override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) =
    decoder.escapeText(event.text, outputType, this)

  private fun addToOutput(text: String, outputType: Key<*>) {
    if (outputType === ProcessOutputTypes.STDERR) {
      output.appendStderr(text)
    }
    else {
      output.appendStdout(text)
    }
  }

  override fun processTerminated(event: ProcessEvent) {
    output.exitCode = event.exitCode
  }

  override fun coloredTextAvailable(text: String, attributes: Key<*>) =
    addToOutput(text, attributes)
}
