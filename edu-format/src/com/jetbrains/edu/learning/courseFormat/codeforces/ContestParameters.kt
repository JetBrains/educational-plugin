package com.jetbrains.edu.learning.courseFormat.codeforces

import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesUserInfo
import java.time.Duration
import java.time.ZonedDateTime

data class ContestParameters(
  val id: Int,
  val languageId: String = "TEXT",
  val languageVersion: String? = null,
  val programTypeId: String? = null,
  val locale: String = "en",
  val endDateTime: ZonedDateTime? = null,
  val name: String = "",
  val codeforcesLanguageRepresentation: String? = null,
  val startDate: ZonedDateTime? = null,
  val length: Duration = Duration.ZERO,
  val registrationLink: String? = null,
  val registrationCountdown: Duration? = null,
  val availableLanguages: List<String> = emptyList(),
  val participantsNumber: Int = 0,
  val standingsLink: String? = null,
  val remainingTime: Duration? = null,
  val authors: List<CodeforcesUserInfo> = emptyList()
)
