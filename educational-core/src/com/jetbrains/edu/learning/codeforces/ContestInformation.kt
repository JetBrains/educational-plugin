package com.jetbrains.edu.learning.codeforces

import java.time.ZonedDateTime

data class ContestInformation(
  val id: Int,
  val name: String,
  val availableLanguages: List<String>,
  val endDateTime: ZonedDateTime
)
