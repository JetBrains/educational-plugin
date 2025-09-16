package com.jetbrains.edu.coursecreator.validation

sealed class ValidationResultNode {

  abstract val isFailed: Boolean

  companion object {
    const val ROOT_NODE_NAME = "root_node"
  }
}

data class ValidationSuite(val name: String, val children: List<ValidationResultNode>) : ValidationResultNode() {
  override val isFailed: Boolean
    get() = children.any { it.isFailed }
}

data class ValidationCase(val name: String, val result: ValidationCaseResult) : ValidationResultNode() {
  override val isFailed: Boolean
    get() = result is ValidationCaseResult.Failed
}

sealed class ValidationCaseResult {
  object Success : ValidationCaseResult()
  data class Ignored(val message: String) : ValidationCaseResult()
  data class Failed(val message: String, val details: String? = null, val diff: ValidationDiff? = null) : ValidationCaseResult()
}

data class ValidationDiff(val expected: String, val actual: String)

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
