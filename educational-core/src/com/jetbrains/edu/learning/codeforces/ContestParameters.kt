package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.fileTypes.PlainTextLanguage
import java.time.Duration
import java.time.ZonedDateTime

data class ContestParameters(
  val id: Int,
  val languageId: String = PlainTextLanguage.INSTANCE.id,
  val locale: String = "en",
  val endDateTime: ZonedDateTime? = null,
  val name: String = "",
  val codeforcesLanguageRepresentation: String? = null,
  val startDate: ZonedDateTime? = null,
  val length: Duration = Duration.ZERO,
  val isRegistrationOpen: Boolean = false,
  val availableLanguages: List<String> = emptyList()
)
