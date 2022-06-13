package com.jetbrains.edu.learning.codeforces.courseFormat

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.codeforces.CodeforcesContestConnector.getContestURLFromID
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_COURSE_TYPE
import com.jetbrains.edu.learning.codeforces.ContestParameters
import com.jetbrains.edu.learning.compatibility.CourseCompatibility
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls
import org.jsoup.nodes.Document
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*
import javax.swing.Icon

open class CodeforcesCourse : Course {
  var endDateTime: ZonedDateTime? = null
  var startDate: ZonedDateTime? = null
  var length: Duration = Duration.ZERO
  var registrationLink: String? = null
  var availableLanguages: List<String> = emptyList()
  var programTypeId: String? = null
    get() = if (field != null) field else CodeforcesTask.codeforcesDefaultProgramTypeId(this).toString()

  var participantsNumber: Int = 0
  var registrationCountdown: Duration? = null
  var standingsLink: String? = null
  var remainingTime: Duration? = Duration.ZERO

  val isUpcomingContest: Boolean get() = registrationCountdown != null || isOngoing
  val isRegistrationOpen: Boolean get() = registrationLink != null
  val isPastContest: Boolean get() = !isUpcomingContest

  val isOngoing: Boolean
    get() = (startDate != null && ZonedDateTime.now().isAfter(startDate)) && (endDateTime?.isAfter(ZonedDateTime.now()) == true)

  //used for deserialization
  constructor()

  constructor(contestParameters: ContestParameters, doc: Document) {
    setContestParameters(contestParameters)

    parseResponseToAddContent(doc)
  }

  constructor(contestParameters: ContestParameters) {
    setContestParameters(contestParameters)
  }

  private fun setContestParameters(contestParameters: ContestParameters) {
    id = contestParameters.id
    programmingLanguage = contestParameters.languageId
    languageCode = contestParameters.locale
    programTypeId = contestParameters.programTypeId
    endDateTime = contestParameters.endDateTime
    updateDate = Date()
    startDate = contestParameters.startDate
    length = contestParameters.length
    registrationLink = contestParameters.registrationLink
    registrationCountdown = contestParameters.registrationCountdown
    availableLanguages = contestParameters.availableLanguages
    name = contestParameters.name
    participantsNumber = contestParameters.participantsNumber
    standingsLink = contestParameters.standingsLink
    remainingTime = contestParameters.remainingTime
    authors = contestParameters.authors
  }

  override val icon: Icon
    get() = EducationalCoreIcons.CODEFORCES_SMALL

  override val itemType: String = CODEFORCES_COURSE_TYPE
  override val checkAction: CheckAction
    get() = CheckAction(EduCoreBundle.lazyMessage("action.codeforces.run.local.tests.text"))

  override val compatibility: CourseCompatibility
    get() = CourseCompatibility.Compatible

  fun getContestUrl(): String = getContestURLFromID(id)

  private fun parseResponseToAddContent(doc: Document) {
    @NonNls val error = "Parsing failed. Unable to find CSS elements:"
    name = doc.selectFirst(".caption")?.text() ?: error("$error caption")
    val problems = doc.select(".problem-statement") ?: error("$error problem-statement")

    description = problems.joinToString("\n") {
      it.select("div.header").select("div.title").text() ?: error("$error div.header, div.title")
    }

    val lesson = Lesson()
    lesson.name = CodeforcesNames.CODEFORCES_PROBLEMS
    lesson.parent = this

    addLesson(lesson)
    problems.forEachIndexed { index, task -> lesson.addTask(CodeforcesTask.create(task, lesson, index + 1)) }
  }
}