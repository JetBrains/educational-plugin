package com.jetbrains.edu.cpp.codeforces

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.CodeforcesTestCase
import com.jetbrains.edu.learning.codeforces.ContestParameters
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.Lesson
import org.jsoup.Jsoup

class CppCodeforcesTaskNamesTest : CodeforcesTestCase() {
  fun `test codeforces contest Global Round 8 Cpp`() {
    val doc = Jsoup.parse(loadText("Contest 1368.html"))
    val course = CodeforcesCourse(ContestParameters(1368, EduNames.CPP), doc)
    val lesson = Lesson().apply { this.course = course }
    course.addLesson(lesson)

    val task = course.findTask("Problems", "task1") as CodeforcesTask
    assertEquals("A. C+=", task.presentableName)
    assertEquals("https://codeforces.com/contest/1368/problem/A?locale=en", task.feedbackLink.link)
  }
}
