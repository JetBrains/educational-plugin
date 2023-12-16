package com.jetbrains.edu.learning.codeforces

enum class TaskTextLanguage(val locale: String) {
  ENGLISH("en") {
    override fun toString() = "English"
  },
  RUSSIAN("ru") {
    override fun toString() = "Русский"
  };
}