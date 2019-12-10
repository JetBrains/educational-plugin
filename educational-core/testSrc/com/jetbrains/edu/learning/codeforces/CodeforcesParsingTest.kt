package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.Lesson
import org.jsoup.Jsoup
import java.io.File
import java.io.IOException

const val expectedTaskDescriptionFile = "expected task description.html"

class CodeforcesParsingTest : EduTestCase() {
  override fun getTestDataPath(): String = "testData/codeforces"

  fun `test codeforces task`() {
    val course = CodeforcesCourse().apply {
      id = 1211
      languageCode = "en"
    }
    val lesson = Lesson().apply { this.course = course }
    course.addLesson(lesson)

    val htmlElement = Jsoup.parse(loadHtmlText()).select(".problem-statement").first()
    val task = CodeforcesTask.create(htmlElement, lesson)

    assertEquals("A. Three Problems", task.name)
    assertEquals("https://codeforces.com/contest/1211/problem/A?locale=en", task.feedbackLink.link)

    val expectedTaskDescription = loadText(expectedTaskDescriptionFile)
    assertEquals(expectedTaskDescription.trim(), task.descriptionText.trim())
  }

  fun `test codeforces course`() {
    val doc = Jsoup.parse(loadHtmlText())
    val course = CodeforcesCourse(ContestURLInfo(1211, "en", EduNames.KOTLIN), doc)

    assertEquals("Kotlin Heroes: Episode 2", course.name)
    assertEquals("""
      A. Three Problems
      B. Traveling Around the Golden Ring of Berland
      C. Ice Cream
      D. Teams
      E. Double Permutation Inc.
      F. kotlinkotlinkotlinkotlin...
      G. King's Path
      H. Road Repair in Treeland
      I. Unusual Graph
    """.trimIndent(), course.description)

    assertEquals(1, course.lessons.size)
    val lesson = course.lessons.first()
    assertEquals(9, lesson.taskList.size)

    assertEquals("A. Three Problems", lesson.taskList[0].name)
    val expectedTaskDescription = loadText(expectedTaskDescriptionFile)
    assertEquals(expectedTaskDescription.trim(), lesson.taskList[0].descriptionText.trim())
    assertEquals("F. kotlinkotlinkotlinkotlin...", lesson.taskList[5].name)
    assertEquals("https://codeforces.com/contest/1211/problem/H?locale=en", lesson.taskList[7].feedbackLink.link)
  }

  @Throws(IOException::class)
  private fun loadText(fileName: String): String {
    return FileUtil.loadFile(File(testDataPath, fileName), true)
  }

  @Throws(IOException::class)
  private fun loadHtmlText(): String {
    return loadText(getTestFile())
  }

  private fun getTestFile(): String {
    return getTestName(true).trim() + ".html"
  }
}