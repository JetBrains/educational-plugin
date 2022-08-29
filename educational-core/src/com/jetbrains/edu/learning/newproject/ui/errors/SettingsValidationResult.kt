package com.jetbrains.edu.learning.newproject.ui.errors

sealed class SettingsValidationResult {
  object Pending : SettingsValidationResult()

  class Ready(val validationMessage: ValidationMessage?) : SettingsValidationResult()

  companion object {
    val OK = Ready(null)
  }
}