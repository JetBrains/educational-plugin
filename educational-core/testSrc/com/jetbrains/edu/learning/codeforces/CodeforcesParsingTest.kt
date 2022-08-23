package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.util.Disposer
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.Lesson
import org.jsoup.Jsoup
import java.util.*

class CodeforcesParsingTest : CodeforcesTestCase() {

  override fun setUp() {
    super.setUp()
    val default = TimeZone.getDefault()
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    Disposer.register(testRootDisposable) {
      TimeZone.setDefault(default)
    }
  }

  fun `test codeforces contest Kotlin Heroes Episode 2 task A`() {
    val course = CodeforcesCourse().apply {
      id = 1211
      languageCode = "en"
    }
    val lesson = Lesson().apply { parent = course }
    course.addLesson(lesson)

    val htmlElement = Jsoup.parse(loadText(contest1211)).select(".problem-statement").first()
    val task = CodeforcesTask.create(htmlElement, lesson, 1)

    assertEquals("A. Three Problems", task.name)
    assertEquals("https://codeforces.com/contest/1211/problem/A?locale=en", task.feedbackLink)
    task.checkTaskDescription(1211, 'A')
  }

  fun `test codeforces contest Kotlin Heroes Episode 2 task G with image`() {
    val course = CodeforcesCourse().apply {
      id = 1211
      languageCode = "en"
    }
    val lesson = Lesson().apply { parent = course }
    course.addLesson(lesson)

    val htmlElement = Jsoup.parse(loadText(contest1211)).select(".problem-statement")[6]
    val task = CodeforcesTask.create(htmlElement, lesson, 1)

    assertEquals("G. King's Path", task.name)
    assertEquals("https://codeforces.com/contest/1211/problem/G?locale=en", task.feedbackLink)
    task.checkTaskDescription(1211, 'G')
  }

  fun `test codeforces contest Kotlin Heroes Episode 2`() {
    val doc = Jsoup.parse(loadText(contest1211))
    val course = CodeforcesCourse(ContestParameters(1211, EduNames.KOTLIN), doc)

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

    assertEquals("https://codeforces.com/contest/1211/problem/H?locale=en", tasks[7].feedbackLink)
  }


  fun testOldInputSampleFormat() {
    val course = CodeforcesCourse().apply {
      id = 1211
      languageCode = "en"
    }
    val lesson = Lesson().apply { parent = course }
    course.addLesson(lesson)

    val htmlElement = Jsoup.parse(loadText(contest1211)).select(".problem-statement")[0]
    val task = CodeforcesTask.create(htmlElement, lesson, 1)

    assertEquals("6\n" +
                 "3 1 4 1 5 9", task.taskFiles["testData/1/input.txt"]?.text)
  }

  fun testNewInputSampleFormat() {
    val course = CodeforcesCourse().apply {
      id = 1715
      languageCode = "en"
    }
    val lesson = Lesson().apply { parent = course }
    course.addLesson(lesson)

    val htmlElement = Jsoup.parse(loadText(contest1715)).select(".problem-statement")[0]
    val task = CodeforcesTask.create(htmlElement, lesson, 1)

    assertEquals("7\n" +
                 "7 5\n" +
                 "5 7\n" +
                 "1 1\n" +
                 "100000 100000\n" +
                 "57 228\n" +
                 "1 5\n" +
                 "5 1", task.taskFiles["testData/1/input.txt"]?.text)
  }

  fun testUpcomingContests() {
    val htmlText = getHtmlText()
    val document = Jsoup.parse(htmlText)
    val upcomingContests = CodeforcesContestConnector.getUpcomingContests(document)
    assertTrue(upcomingContests.size == 4)

    val firstContest = upcomingContests.first()
    assertEquals(1492, firstContest.id)
    assertEquals("Codeforces Round #704 (Div. 2)", firstContest.name)
    assertEquals("2021-02-23T09:05Z[UTC]", firstContest.startDate.toString())
    assertEquals(120, firstContest.length.toMinutes())
    assertNotNull(firstContest.registrationCountdown)
  }

  fun testUpcomingContestsRegistrationLink() {
    val htmlText = getHtmlText()
    val document = Jsoup.parse(htmlText)
    val upcomingContests = CodeforcesContestConnector.getUpcomingContests(document)
    assertTrue(upcomingContests.size == 4)

    val contest = upcomingContests[3]
    assertEquals("/contestRegistration/1488", contest.registrationLink)
  }

  fun testUpcomingContestsRegistrationDate() {
    val htmlText = getHtmlText()
    val document = Jsoup.parse(htmlText)
    val upcomingContests = CodeforcesContestConnector.getUpcomingContests(document)
    assertTrue(upcomingContests.size == 4)

    val contest = upcomingContests[3]
    val registrationCountdown = contest.registrationCountdown!!
    assertEquals(18, registrationCountdown.toDays())
  }

  fun testRecentContests() {
    val htmlText = getHtmlText()
    val document = Jsoup.parse(htmlText)
    val recentContests = CodeforcesContestConnector.getRecentContests(document)
    assertTrue(recentContests.size == 100)

    val firstContest = recentContests.first()
    assertEquals(1486, firstContest.id)
    assertEquals("Codeforces Round #703 (Div. 2)", firstContest.name)
    assertEquals("2021-02-18T14:35Z[UTC]", firstContest.startDate.toString())
    assertEquals(135, firstContest.length.toMinutes())
    assertNull(firstContest.registrationLink)
  }

  fun testContestWinterTime() {
    TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
    val htmlText = getHtmlText()
    val document = Jsoup.parse(htmlText)
    val recentContests = CodeforcesContestConnector.getRecentContests(document)
    assertTrue(recentContests.size == 100)

    val firstContest = recentContests.first()

    assertEquals("2021-02-18T15:35+01:00[Europe/Berlin]", firstContest.startDate.toString())
  }

  fun testContestSummerTime() {
    TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
    val htmlText = getHtmlText()
    val document = Jsoup.parse(htmlText)
    val recentContests = CodeforcesContestConnector.getRecentContests(document)
    assertTrue(recentContests.size == 100)

    val firstContest = recentContests.first()

    assertEquals("2021-05-18T16:35+02:00[Europe/Berlin]", firstContest.startDate.toString())
  }

  fun testParticipantsNumber() {
    val htmlText = getHtmlText()
    val document = Jsoup.parse(htmlText)
    val recentContests = CodeforcesContestConnector.getRecentContests(document)
    assertTrue(recentContests.size == 100)

    val firstContest = recentContests.first()

    assertEquals(20195, firstContest.participantsNumber)
  }

  fun testUpcomingContestParticipantsNumber() {
    val htmlText = getHtmlText()
    val document = Jsoup.parse(htmlText)
    val upcomingContests = CodeforcesContestConnector.getUpcomingContests(document)

    val contest = upcomingContests[3]
    assertEquals(1880, contest.participantsNumber)
  }

  fun testUpcomingContestsRegistrationCountdown() {
    val htmlText = getHtmlText()
    val document = Jsoup.parse(htmlText)
    val upcomingContests = CodeforcesContestConnector.getUpcomingContests(document)

    // countdown elements format: <span class="countdown" cdid="i3">40:15:33</span>
    val firstContest = upcomingContests[0]
    assertEquals(39, firstContest.registrationCountdown?.toHours()?.toInt())
    // countdown elements format: <span class="countdown" cdid="i5"><span title="510:33:52">3 недели</span></span>
    val secondContest = upcomingContests[1]
    assertEquals(509, secondContest.registrationCountdown?.toHours()?.toInt())
  }

  fun testRunningContestsTimeRemaining() {
    val htmlText = getHtmlText()
    val document = Jsoup.parse(htmlText)
    val upcomingContests = CodeforcesContestConnector.getUpcomingContests(document)

    // countdown elements format: <span class="countdown" cdid="i3">40:15:33</span>
    val firstContest = upcomingContests[0]
    val remainingTime = firstContest.remainingTime
    assertNotNull(remainingTime)
    assertEquals(0, remainingTime!!.toHoursPart())
    assertEquals(29, remainingTime.toMinutesPart())
  }

  fun testCodeforcesAuthors() {
    val htmlText = getHtmlText()
    val document = Jsoup.parse(htmlText)
    val upcomingContests = CodeforcesContestConnector.getUpcomingContests(document)

    // countdown elements format: <span class="countdown" cdid="i3">40:15:33</span>
    val firstContest = upcomingContests[1]
    val authors = firstContest.authors
    assertNotNull(authors)
    assertEquals(7, authors.size)
  }

  private fun getHtmlText(): String = java.io.File("$testDataPath/$testFile").readText()

  private val testFile: String get() = "${getTestName(true)}.html"
}