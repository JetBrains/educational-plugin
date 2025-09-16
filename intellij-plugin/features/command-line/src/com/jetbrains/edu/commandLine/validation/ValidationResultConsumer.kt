package com.jetbrains.edu.commandLine.validation

import com.jetbrains.edu.coursecreator.validation.ValidationSuite

/**
 * Consumes validation results and produces output in a format specific to a concrete implementation.
 */
interface ValidationResultConsumer {
  fun consume(rootNode: ValidationSuite)
}
