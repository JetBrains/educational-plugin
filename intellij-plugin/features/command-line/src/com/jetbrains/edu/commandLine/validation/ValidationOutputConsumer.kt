package com.jetbrains.edu.commandLine.validation

/**
 * Consumes validation output and produces validation result
 */
interface ValidationOutputConsumer {
  fun consume(output: String)
}
