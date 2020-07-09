package com.jetbrains.edu.learning.codeforces

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.Lesson
import org.jsoup.Jsoup

class CodeforcesParsingTest : CodeforcesTestCase() {
  fun `test codeforces contest Kotlin Heroes Episode 2 task A`() {
    val course = CodeforcesCourse().apply {
      id = 1211
      languageCode = "en"
    }
    val lesson = Lesson().apply { this.course = course }
    course.addLesson(lesson)

    val htmlElement = Jsoup.parse(loadText(contest1211)).select(".problem-statement").first()
    val task = CodeforcesTask.create(htmlElement, lesson)

    assertEquals("A. Three Problems", task.name)
    assertEquals("https://codeforces.com/contest/1211/problem/A?locale=en", task.feedbackLink.link)
    task.checkTaskDescription(1211, 'A')
  }

  fun `test codeforces contest Kotlin Heroes Episode 2 task G with image`() {
    val course = CodeforcesCourse().apply {
      id = 1211
      languageCode = "en"
    }
    val lesson = Lesson().apply { this.course = course }
    course.addLesson(lesson)

    val htmlElement = Jsoup.parse(loadText(contest1211)).select(".problem-statement")[6]
    val task = CodeforcesTask.create(htmlElement, lesson)

    assertEquals("G. King's Path", task.name)
    assertEquals("https://codeforces.com/contest/1211/problem/G?locale=en", task.feedbackLink.link)
    task.checkTaskDescription(1211, 'G')
  }

  fun `test codeforces contest Kotlin Heroes Episode 2`() {
    val doc = Jsoup.parse(loadText(contest1211))
    val course = CodeforcesCourse(ContestParameters(1211, "en", "Kotlin", EduNames.KOTLIN), doc)

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
    val tasks = course.lessons.first().taskList
    assertEquals(9, tasks.size)

    assertEquals("A. Three Problems", tasks[0].name)
    assertEquals("B. Traveling Around the Golden Ring of Berland", tasks[1].name)
    assertEquals("C. Ice Cream", tasks[2].name)
    assertEquals("D. Teams", tasks[3].name)
    assertEquals("E. Double Permutation Inc.", tasks[4].name)
    assertEquals("F. kotlinkotlinkotlinkotlin...", tasks[5].name)
    assertEquals("G. King's Path", tasks[6].name)
    assertEquals("H. Road Repair in Treeland", tasks[7].name)
    assertEquals("I. Unusual Graph", tasks[8].name)

    assertEquals("https://codeforces.com/contest/1211/problem/H?locale=en", tasks[7].feedbackLink.link)
  }
}