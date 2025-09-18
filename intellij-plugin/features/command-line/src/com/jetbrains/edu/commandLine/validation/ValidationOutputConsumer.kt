package com.jetbrains.edu.commandLine.validation

/**
 * Consumes validation output and produces validation result
 */
interface ValidationOutputConsumer : AutoCloseable {
  fun consume(output: String)
}
