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
