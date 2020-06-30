package com.jetbrains.edu.learning.codeforces

import java.time.ZonedDateTime

data class ContestParameters(
  val id: Int,
  val languageId: String,
  val locale: String = "en",
  val endDateTime: ZonedDateTime? = null,
  val codeforcesLanguageRepresentation: String? = null
)
