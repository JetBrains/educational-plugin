package com.jetbrains.edu.coursecreator.validation

@DslMarker
annotation class ValidationDsl

@ValidationDsl
class ValidationTreeBuilder(private val name: String) {

  private val children = mutableListOf<ValidationResultNode>()

  suspend fun validationSuit(name: String, block: suspend ValidationTreeBuilder.() -> Unit) {
    children += withValidationTreeBuilder(name, block)
  }

  fun validationCase(name: String, result: ValidationCaseResult) {
    children += ValidationCase(name, result)
  }

  fun build(): ValidationSuite {
    return ValidationSuite(name, children)
  }
}

suspend fun withValidationTreeBuilder(name: String, block: suspend ValidationTreeBuilder.() -> Unit): ValidationSuite {
  val builder = ValidationTreeBuilder(name)
  builder.block()
  return builder.build()
}
