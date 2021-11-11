package com.jetbrains.edu.learning.checker

enum class CheckResultSeverity {
  Info, Warning, Error;

  fun isWaring() = this == Warning
  fun isInfo() = this == Info
}