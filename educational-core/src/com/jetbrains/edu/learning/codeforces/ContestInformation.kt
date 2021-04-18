package com.jetbrains.edu.learning.codeforces

import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import java.time.Duration
import java.time.ZonedDateTime

class ContestInformation(
  id: Int,
  name: String,
  endDateTime: ZonedDateTime,
  val startDate: ZonedDateTime? = null,
  val length: Duration = Duration.ZERO,
  val isRegistrationOpen: Boolean = false,
  val availableLanguages: List<String> = emptyList()
) : CodeforcesCourse() {
  init {
    this.id = id
    this.name = name
    this.endDateTime = endDateTime
  }
}
