package com.jetbrains.edu.learning.courseFormat.tasks.choice

/**
 * Choice tasks created on Stepik can't be checked locally, so they have [UNKNOWN] status
 */
enum class ChoiceOptionStatus {
  CORRECT, INCORRECT, UNKNOWN
}