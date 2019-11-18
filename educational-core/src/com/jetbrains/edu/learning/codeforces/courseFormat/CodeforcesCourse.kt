package com.jetbrains.edu.learning.codeforces.courseFormat

import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.codeforces.CodeforcesContestConnector.getContestURLFromID
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_COURSE_TYPE
import com.jetbrains.edu.learning.codeforces.ContestURLInfo
import com.jetbrains.edu.learning.courseFormat.CourseCompatibility
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import icons.EducationalCoreIcons
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import javax.swing.Icon

class CodeforcesCourse : EduCourse {
  @Suppress("unused") //used for deserialization
  constructor()

  constructor(contestURLInfo: ContestURLInfo, html: ResponseBody) {
    id = contestURLInfo.id
    language = contestURLInfo.languageId
    languageCode = contestURLInfo.locale

    parseResponseToAddContent(html)
  }

  val contestUrl: String by lazy { getContestURLFromID(id) }
  val submissionUrl: String by lazy { "$contestUrl/submit?locale=${languageCode}" }

  override fun courseCompatibility(courseInfo: EduCourse): CourseCompatibility = CourseCompatibility.COMPATIBLE
  override fun getIcon(): Icon = EducationalCoreIcons.Codeforces
  override fun getItemType(): String = CODEFORCES_COURSE_TYPE
  override fun getCheckAction(): CheckAction = CheckAction(CodeforcesNames.RUN_LOCAL_TESTS)

  private fun parseResponseToAddContent(html: ResponseBody) {
    val doc = Jsoup.parse(html.string())
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