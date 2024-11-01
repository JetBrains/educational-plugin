package com.jetbrains.edu.aiHints.core.context

data class FunctionParameter(
  val name: String,
  val type: String
) {
  override fun toString() = "$name: $type"
}