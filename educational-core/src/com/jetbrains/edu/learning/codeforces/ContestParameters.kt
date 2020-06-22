package com.jetbrains.edu.learning.codeforces

data class ContestParameters(
  val id: Int,
  val languageId: String,
  val locale: String,
  val codeforcesLanguageRepresentation: String? = null
)
