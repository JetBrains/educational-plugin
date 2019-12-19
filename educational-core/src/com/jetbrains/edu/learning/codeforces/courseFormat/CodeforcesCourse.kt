package com.jetbrains.edu.learning.codeforces.courseFormat

import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.codeforces.CodeforcesContestConnector.getContestURLFromID
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_COURSE_TYPE
import com.jetbrains.edu.learning.codeforces.ContestURLInfo
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import icons.EducationalCoreIcons
import org.jsoup.nodes.Document
import javax.swing.Icon

class CodeforcesCourse : Course {
  @Suppress("unused") //used for deserialization
  constructor()

  constructor(contestURLInfo: ContestURLInfo, doc: Document) {
    id = contestURLInfo.id
    language = contestURLInfo.languageId
    languageCode = contestURLInfo.locale

    parseResponseToAddContent(doc)
  }

  val contestUrl: String by lazy { getContestURLFromID(id) }
  val submissionUrl: String by lazy { "$contestUrl/submit?locale=${languageCode}" }

  override fun getIcon(): Icon = EducationalCoreIcons.Codeforces
  override fun getItemType(): String = CODEFORCES_COURSE_TYPE
  override fun getCheckAction(): CheckAction = CheckAction(CodeforcesNames.RUN_LOCAL_TESTS)

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
    problems.forEach { lesson.addTask(CodeforcesTask.create(it, lesson)) }
  }
}