package com.jetbrains.edu.learning.courseFormat.eduAssistant

data class FunctionSignature(
  val name: String,
  val parameters: List<FunctionParameter>,
  val returnType: String,
  val signatureSource: SignatureSource? = null,
  val bodyLineCount: Int? = null
) {
  override fun toString(): String {
    return "$name$NAME_ARGS_SEPARATOR${parameters.joinToString(separator = ARGS_SEPARATOR)}$ARGS_RETURN_TYPE_SEPARATOR$returnType"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as FunctionSignature

    if (name != other.name) return false
    if (parameters != other.parameters) return false

    return true
  }

  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + parameters.hashCode()
    return result
  }

  companion object {
    private const val NAME_ARGS_SEPARATOR = "("
    private const val ARGS_RETURN_TYPE_SEPARATOR = "): "
    private const val ARGS_SEPARATOR = ", "
  }
}

data class FunctionParameter(
  val name: String,
  val type: String
) {
  override fun toString() = "${name}$NAME_TYPE_SEPARATOR$type"

  companion object {
    private const val NAME_TYPE_SEPARATOR = ": "
  }
}

enum class SignatureSource {
  HIDDEN_FILE,
  VISIBLE_FILE,
  MODEL_SOLUTION,
  GENERATED_SOLUTION
}
