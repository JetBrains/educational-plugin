package com.jetbrains.edu.assistant.validation.accuracy

abstract class AccuracyCalculator<T> {
  abstract fun calculateValidationAccuracy(manualRecords: List<T>, autoRecords: List<T>): T

  abstract fun calculateOverallAccuracy(records: List<T>): T
}
