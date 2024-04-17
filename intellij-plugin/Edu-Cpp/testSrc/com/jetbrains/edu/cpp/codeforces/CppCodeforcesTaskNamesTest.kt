package com.jetbrains.edu.cpp.codeforces

import com.jetbrains.edu.learning.codeforces.CodeforcesTestCase
import com.jetbrains.edu.learning.codeforces.api.parseResponseToAddContent
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CPP
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesCourse
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.codeforces.ContestParameters
import com.jetbrains.edu.learning.findTask
import org.jsoup.Jsoup
import org.junit.Test
import java.time.ZonedDateTime

class CppCodeforcesTaskNamesTest : CodeforcesTestCase() {
  @Test
  fun `test codeforces contest Global Round 8 Cpp`() {
    val doc = Jsoup.parse(loadText("Contest 1368.html"))
    val course = CodeforcesCourse(ContestParameters(1368, CPP, "50", startDate = ZonedDateTime.now()))
    course.parseResponseToAddContent(doc)
    val lesson = Lesson().apply { parent = course }
    course.addLesson(lesson)

    val task = course.findTask("Problems", "task1") as CodeforcesTask
    assertEquals("A. C+=", task.presentableName)
    assertEquals("A", task.problemIndex)
    assertEquals("https://codeforces.com/contest/1368/problem/A?locale=en", task.feedbackLink)
  }
}
