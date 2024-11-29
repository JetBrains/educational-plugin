package com.jetbrains.edu.cognifire.models

class CodeExpression (
  val code: String,
  private val baseContentOffset: Int,
  private val baseStartOffset: Int,
  private val baseEndOffset: Int
): ProdeExpression {
  override var dynamicStartOffset: Int = 0
  override var dynamicEndOffset: Int = 0

  override val contentOffset: Int
    get() = baseContentOffset + dynamicStartOffset

  override val startOffset: Int
    get() = baseStartOffset + dynamicStartOffset

  override val endOffset: Int
    get() = baseEndOffset + dynamicEndOffset

  override fun shiftStartOffset(delta: Int) {
    dynamicStartOffset += delta
  }

  override fun shiftEndOffset(delta: Int) {
    dynamicEndOffset += delta
  }
}
