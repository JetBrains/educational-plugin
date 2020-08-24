package com.jetbrains.edu.learning.codeforces.courseFormat

import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.codeforces.CodeforcesContestConnector.getContestURLFromID
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_COURSE_TYPE
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_SUBMIT
import com.jetbrains.edu.learning.codeforces.ContestParameters
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import icons.EducationalCoreIcons
import org.jsoup.nodes.Document
import java.time.ZonedDateTime
import java.util.*
import javax.swing.Icon

class CodeforcesCourse : Course {
  var endDateTime: ZonedDateTime? = null

  @Suppress("unused") //used for deserialization
  constructor()

  constructor(contestParameters: ContestParameters, doc: Document) {
    id = contestParameters.id
    language = contestParameters.languageId
    languageCode = contestParameters.locale
    endDateTime = contestParameters.endDateTime
    updateDate = Date()

    parseResponseToAddContent(doc)
  }

  override fun getIcon(): Icon = EducationalCoreIcons.Codeforces
  override fun getId(): Int = myId
  override fun getItemType(): String = CODEFORCES_COURSE_TYPE
  override fun getCheckAction(): CheckAction = CheckAction(CodeforcesNames.RUN_LOCAL_TESTS)

  fun getContestUrl(): String = getContestURLFromID(id)
  fun getSubmissionUrl(): String = "${getContestUrl()}/${CODEFORCES_SUBMIT}?locale=$languageCode"
  fun isOngoing(): Boolean = if (endDateTime == null) false else (endDateTime!! > ZonedDateTime.now())

  private fun parseResponseToAddContent(doc: Document) {
    name = doc.selectFirst(".caption").text()

    val problems = doc.select(".problem-statement")

    description = problems.joinToString("\n") {
      it.select("div.header").select("div.title").text()
    }

    val lesson = Lesson()
    lesson.name = CodeforcesNames.CODEFORCES_PROBLEMS
    lesson.course = this

    addLesson(lesson)
    problems.forEachIndexed { index, task -> lesson.addTask(CodeforcesTask.create(task, lesson, index + 1)) }
  }
}