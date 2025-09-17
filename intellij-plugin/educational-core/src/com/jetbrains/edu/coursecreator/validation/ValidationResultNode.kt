package com.jetbrains.edu.coursecreator.validation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ValidationResultNode {

  abstract val isFailed: Boolean

  companion object {
    const val ROOT_NODE_NAME = "root_node"
  }
}

@Serializable
@SerialName("suite")
data class ValidationSuite(val name: String, val children: List<ValidationResultNode>) : ValidationResultNode() {
  override val isFailed: Boolean
    get() = children.any { it.isFailed }
}

@Serializable
@SerialName("case")
data class ValidationCase(val name: String, val result: ValidationCaseResult) : ValidationResultNode() {
  override val isFailed: Boolean
    get() = result is ValidationCaseResult.Failed
}

@Serializable
sealed class ValidationCaseResult {
  @Serializable
  @SerialName("success")
  object Success : ValidationCaseResult()
  @Serializable
  @SerialName("ignored")
  data class Ignored(val message: String) : ValidationCaseResult()
  @Serializable
  @SerialName("failed")
  data class Failed(val message: String, val details: String? = null, val diff: ValidationDiff? = null) : ValidationCaseResult()
}

@Serializable
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
