package com.jetbrains.edu.learning.codeforces

import java.time.Duration
import java.time.ZonedDateTime

data class ContestInformation(
  val id: Int,
  val name: String,
  val endDateTime: ZonedDateTime,
  val startDate: ZonedDateTime? = null,
  val length: Duration = Duration.ZERO,
  val isRegistrationOpen: Boolean = false,
  val availableLanguages: List<String> = emptyList()
)
