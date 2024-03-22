package com.jetbrains.edu.assistant.validation.util

fun calculateCriterionResultAccuracy(
  record: (Int) -> String,
  size: Int,
  compareAction: (String) -> Boolean
) = calculateCriterionAccuracy({ _ -> null }, record, size, { f, _ -> compareAction(f) })

fun calculateCriterionAccuracy(
  manualRecord: (Int) -> String?,
  autoRecord: (Int) -> String,
  size: Int,
  compareAction: (String, String?) -> Boolean
): String {
  var matchCount = 0
  var totalCount = 0
  for (i in 0 until size) {
    if (autoRecord(i).isNotBlank()) {
      if (compareAction(autoRecord(i), manualRecord(i))) {
        matchCount++
      }
      totalCount++
    }
  }
  return "${matchCount.toDouble() / totalCount * PERCENT_DENOMINATOR}"
}

fun compareCriterion(first: String, second: String, firstOption: String = YES_KEYWORD, secondOption: String = NO_KEYWORD) =
  (first.startsWith(firstOption, ignoreCase = true) && second.startsWith(firstOption, ignoreCase = true)) ||
  (first.startsWith(secondOption, ignoreCase = true) && second.startsWith(secondOption, ignoreCase = true))

fun isCorrectAnswer(answer: String, option: String = YES_KEYWORD) = answer.startsWith(option, ignoreCase = true)


const val PERCENT_DENOMINATOR = 100
const val ACCURACY_KEYWORD = "Accuracy:"
const val YES_KEYWORD = "yes"
const val NO_KEYWORD = "no"
const val CODING_KEYWORD = "Coding"
const val NOT_CODING_KEYWORD = "Not coding"
const val BOH_KEYWORD = "BOH"
const val HLD_KEYWORD = "HLD"
const val NEW_KEYWORD = "new"
const val CHANGED_KEYWORD = "changed"
const val DELETED_KEYWORD = "deleted"
