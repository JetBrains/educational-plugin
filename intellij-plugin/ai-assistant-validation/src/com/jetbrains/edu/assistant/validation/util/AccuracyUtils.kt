package com.jetbrains.edu.assistant.validation.util

fun calculateCriterionAccuracy(
  manualRecord: (Int) -> String,
  autoRecord: (Int) -> String,
  size: Int,
  compareAction: (String, String) -> Boolean
): String {
  var matchCount = 0
  var totalCount = 0
  for (i in 0 until size) {
    if (autoRecord(i).isNotBlank()) {
      if (compareAction(manualRecord(i), autoRecord(i))) {
        matchCount++
      }
      totalCount++
    }
  }
  return "${matchCount.toDouble() / totalCount * PERCENT_DENOMINATOR}"
}

fun compareCriterion(first: String, second: String, firstAnswer: String = "yes", secondAnswer: String = "no") =
  (first.startsWith(firstAnswer, ignoreCase = true) && second.startsWith(firstAnswer, ignoreCase = true)) ||
  (first.startsWith(secondAnswer, ignoreCase = true) && second.startsWith(secondAnswer, ignoreCase = true))


const val PERCENT_DENOMINATOR = 100
const val ACCURACY_KEYWORD = "Accuracy:"
const val CODING_KEYWORD = "Coding"
const val NOT_CODING_KEYWORD = "Not coding"
const val BOH_KEYWORD = "BOH"
const val HLD_KEYWORD = "HLD"
const val NEW_KEYWORD = "new"
const val CHANGED_KEYWORD = "changed"
const val DELETED_KEYWORD = "deleted"
