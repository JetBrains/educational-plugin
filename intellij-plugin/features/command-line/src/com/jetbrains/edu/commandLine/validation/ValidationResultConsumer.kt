package com.jetbrains.edu.commandLine.validation

import com.jetbrains.edu.coursecreator.validation.ValidationSuite

/**
 * Consumes validation results and produces output in a format specific to a concrete implementation.
 */
abstract class ValidationResultConsumer(protected val outputConsumer: ValidationOutputConsumer) {
  abstract fun consume(rootNode: ValidationSuite)
}
