package com.jetbrains.edu.learning.courseFormat

object Language {

  // id to presentable name
  private val languages = mapOf(
    "Python" to "Python",
    "ObjectiveC" to "C/C++",
    "go" to "Go",
    "JAVA" to "Java",
    "kotlin" to "Kotlin",
    "Scala" to "Scala",
    "JavaScript" to "JavaScript",
    "Rust" to "Rust",
    "PHP" to "PHP",
    "Shell Script" to "Shell Script",
    "SQL" to "SQL",
    "C#" to "C#",
    "unity" to "unity",

    // used only for testing
    "TEXT" to "Plain text",
    "FakeGradleBasedLanguage" to "FakeGradleBasedLanguage"
  )

  fun findLanguageByID(id: String): String? {
    return languages[id]
  }

  fun findLanguageByName(name: String): String? {
    return languages.filter { it.value == name }.keys.firstOrNull()
  }
}