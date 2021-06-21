package com.jetbrains.edu.learning.codeforces

import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.courseFormat.Tag
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

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

    val codeforcesLink = "<a href='${CodeforcesContestConnector.getContestURLFromID(id)}'>${CodeforcesNames.CODEFORCES_TITLE}</a>"
    val eduToolsLink = "<a href='${CodeforcesNames.CODEFORCES_EDU_TOOLS_HELP}'>EduTools</a>"
    val date = endDateTime.toLocalDate().format(DateTimeFormatter.ofPattern("dd MMM YYYY", Locale.ENGLISH))
    this.description = EduCoreBundle.message("codeforces.past.course.description", date, codeforcesLink, eduToolsLink)
  }

  override fun getTags(): List<Tag> {
    return emptyList()
  }
}
