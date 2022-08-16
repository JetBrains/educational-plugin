package com.jetbrains.edu.learning.courseFormat

object AnswerPlaceholderComparator : Comparator<AnswerPlaceholder> {
  override fun compare(o1: AnswerPlaceholder, answerPlaceholder: AnswerPlaceholder): Int {
    return o1.offset - answerPlaceholder.offset
  }
}