package com.jetbrains.edu.aiHints.core.context

data class FunctionSignature(
  val name: String,
  val parameters: List<FunctionParameter>,
  val returnType: String,
  val signatureSource: SignatureSource? = null,
  val bodyLineCount: Int? = null
) {
  override fun toString(): String {
    return "$name(${parameters.joinToString(separator = ", ")}): $returnType"
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
}