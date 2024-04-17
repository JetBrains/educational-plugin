package com.jetbrains.edu.slow.integration.codeforces

import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.TEST_DATA_FOLDER
import com.jetbrains.edu.learning.codeforces.CodeforcesTestCase
import com.jetbrains.edu.learning.codeforces.actions.StartCodeforcesContestAction
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.codeforces.api.MockCodeforcesConnector
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CODEFORCES_URL
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesCourse
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesTask.Companion.codeforcesSubmitLink
import com.jetbrains.edu.learning.courseFormat.codeforces.ContestParameters
import org.junit.Test

class CodeforcesLoadingTest : CodeforcesTestCase() {
  override fun setUp() {
    super.setUp()
    val mockCodeforcesConnector = CodeforcesConnector.getInstance() as MockCodeforcesConnector
    mockCodeforcesConnector.setBaseUrl(CODEFORCES_URL, testRootDisposable)
  }

  private val contestKotlinHeroesEpisode1: CodeforcesCourse by lazy {
    val contestInfo = ContestParameters(1170, EduFormatNames.KOTLIN, codeforcesLanguageRepresentation = "Kotlin")
    when (val contest = StartCodeforcesContestAction.getContestUnderProgress(contestInfo)) {
      is Err -> error(contest.error)
      is Ok -> contest.value
    }
  }

  private val contestKotlinHeroesPractice6: CodeforcesCourse by lazy {
    val contestInfo = ContestParameters(1489, EduFormatNames.KOTLIN, codeforcesLanguageRepresentation = "Kotlin")
    when (val contest = StartCodeforcesContestAction.getContestUnderProgress(contestInfo)) {
      is Err -> error(contest.error)
      is Ok -> contest.value
    }
  }

  private val codeforcesRound605Div3: CodeforcesCourse by lazy {
    val contestInfo = ContestParameters(1272, EduFormatNames.JAVA, codeforcesLanguageRepresentation = "Java")
    when (val contest = StartCodeforcesContestAction.getContestUnderProgress(contestInfo)) {
      is Err -> error(contest.error)
      is Ok -> contest.value
    }
  }

  @Test
  fun `test codeforces contest Kotlin Heroes Episode 1`() {
    val contest = contestKotlinHeroesEpisode1

    assertEquals(1170, contest.id)
    assertEquals("en", contest.languageCode)
    assertEquals("Kotlin Heroes: Episode 1", contest.name)
    assertEquals("https://codeforces.com/contest/1170", contest.getContestUrl())
    assertEquals("""
      A. Three Integers Again
      B. Bad Days
      C. Minus and Minus Give Plus
      D. Decoding of Integer Sequences
      E. Sliding Doors
      F. Wheels
      G. Graph Decomposition
      H. Longest Saw
      I. Good Subsets
    """.trimIndent(), contest.description)

    assertEquals(1, contest.lessons.size)
    val tasks = contest.lessons.first().taskList
    assertEquals(9, tasks.size)

    assertEquals("A. Three Integers Again", tasks[0].name)
    assertEquals("B. Bad Days", tasks[1].name)
    assertEquals("C. Minus and Minus Give Plus", tasks[2].name)
    assertEquals("D. Decoding of Integer Sequences", tasks[3].name)
    assertEquals("E. Sliding Doors", tasks[4].name)
    assertEquals("F. Wheels", tasks[5].name)
    assertEquals("G. Graph Decomposition", tasks[6].name)
    assertEquals("H. Longest Saw", tasks[7].name)
    assertEquals("I. Good Subsets", tasks[8].name)

    assertEquals("https://codeforces.com/contest/1170/problem/H?locale=en", tasks[7].feedbackLink)
    assertEquals("https://codeforces.com/contest/1170/submit?locale=en&programTypeId=48&submittedProblemIndex=H",
                 codeforcesSubmitLink(tasks[7] as CodeforcesTask))
  }

  @Test
  fun `test codeforces contest Kotlin Heroes Episode 1 task A`() {
    val contest = contestKotlinHeroesEpisode1
    val task = contest.lessons.first().taskList[0] as CodeforcesTask

    assertEquals("A. Three Integers Again", task.name)
    assertEquals("A", task.problemIndex)
    assertEquals("https://codeforces.com/contest/1170/problem/A?locale=en", task.feedbackLink)
    task.checkTaskDescription(1170, 'A')
  }

  @Test
  fun `test codeforces contest Kotlin Heroes Episode 1 task E with image`() {
    val contest = contestKotlinHeroesEpisode1
    val task = contest.lessons.first().taskList[4] as CodeforcesTask

    assertEquals("E. Sliding Doors", task.name)
    assertEquals("E", task.problemIndex)
    assertEquals("https://codeforces.com/contest/1170/problem/E?locale=en", task.feedbackLink)
    task.checkTaskDescription(1170, 'E')
  }

  @Test
  fun `test codeforces contest Kotlin Heroes Episode 1 task G with image`() {
    val contest = contestKotlinHeroesEpisode1
    val task = contest.lessons.first().taskList[6] as CodeforcesTask

    assertEquals("G. Graph Decomposition", task.name)
    assertEquals("G", task.problemIndex)
    assertEquals("https://codeforces.com/contest/1170/problem/G?locale=en", task.feedbackLink)
    task.checkTaskDescription(1170, 'G')
  }

  @Test
  fun `test codeforces contest Codeforces Round 605 (Div 3)`() {
    val contest = codeforcesRound605Div3

    assertEquals(1272, contest.id)
    assertEquals("en", contest.languageCode)
    assertEquals("Codeforces Round 605 (Div. 3)", contest.name)
    assertEquals("https://codeforces.com/contest/1272", contest.getContestUrl())
    assertEquals("""
      A. Three Friends
      B. Snow Walking Robot
      C. Yet Another Broken Keyboard
      D. Remove One Element
      E. Nearest Opposite Parity
      F. Two Bracket Sequences
    """.trimIndent(), contest.description)

    assertEquals(1, contest.lessons.size)
    val tasks = contest.lessons.first().taskList
    assertEquals(6, tasks.size)

    assertEquals("A. Three Friends", tasks[0].name)
    assertEquals("B. Snow Walking Robot", tasks[1].name)
    assertEquals("C. Yet Another Broken Keyboard", tasks[2].name)
    assertEquals("D. Remove One Element", tasks[3].name)
    assertEquals("E. Nearest Opposite Parity", tasks[4].name)
    assertEquals("F. Two Bracket Sequences", tasks[5].name)

    assertEquals("https://codeforces.com/contest/1272/problem/D?locale=en", tasks[3].feedbackLink)
    assertEquals("https://codeforces.com/contest/1272/submit?locale=en&submittedProblemIndex=D", codeforcesSubmitLink(tasks[3] as CodeforcesTask))
  }

  @Test
  fun `test codeforces contest Codeforces Round 605 (Div 3) task A`() {
    val contest = codeforcesRound605Div3
    val task = contest.lessons.first().taskList[0] as CodeforcesTask

    assertEquals("A. Three Friends", task.name)
    assertEquals("A", task.problemIndex)
    assertEquals("https://codeforces.com/contest/1272/problem/A?locale=en", task.feedbackLink)
    task.checkTaskDescription(1272, 'A')
  }

  @Test
  fun `test codeforces contest Codeforces Round 605 (Div 3) task B with image`() {
    val contest = codeforcesRound605Div3
    val task = contest.lessons.first().taskList[1] as CodeforcesTask

    assertEquals("B. Snow Walking Robot", task.name)
    assertEquals("B", task.problemIndex)
    assertEquals("https://codeforces.com/contest/1272/problem/B?locale=en", task.feedbackLink)
    task.checkTaskDescription(1272, 'B')
  }

  @Test
  fun `test parsing several test samples for contest Kotlin Heroes Practice 6 task A`() {
    val contest = contestKotlinHeroesPractice6

    assertEquals(1489, contest.id)
    assertEquals("en", contest.languageCode)
    assertEquals("Kotlin Heroes: Practice 6", contest.name)

    val taskA = contest.lessons.first().taskList.first() as CodeforcesTask

    val expectedTests = listOf(
      TestSample("6\n1 5 5 1 6 1", "3\n5 6 1"),
      TestSample("5\n2 4 2 4 4", "2\n2 4"),
      TestSample("5\n6 6 6 6 6", "1\n6")
    )

    for ((index, expectedTest) in expectedTests.withIndex()) {
      val testIndex = index + 1
      val input = taskA.taskFiles["${TEST_DATA_FOLDER}/$testIndex/${taskA.inputFileName}"]?.text
                  ?: error("Can't find input file for test №$testIndex")
      val output = taskA.taskFiles["${TEST_DATA_FOLDER}/$testIndex/${taskA.outputFileName}"]?.text
                   ?: error("Can't find output file for test №$testIndex")

      assertEquals(expectedTest.input, input)
      assertEquals(expectedTest.output, output)
    }
  }

  @Test
  fun `test parsing single test sample for contest Kotlin Heroes Practice 6 task B`() {
    val contest = contestKotlinHeroesPractice6

    assertEquals(1489, contest.id)
    assertEquals("en", contest.languageCode)
    assertEquals("Kotlin Heroes: Practice 6", contest.name)

    val taskB = contest.lessons.first().taskList[1] as CodeforcesTask

    val expectedTest = TestSample(
      """
          4
          10 1 3
          7 3 2
          1 1000 1
          1000000000000 42 88""".trimIndent(),
      """
          10
          9
          1000
          42000000000000""".trimIndent()
    )
    // Only 2 files should be here: input and output for 1 test
    assertEquals(2, taskB.taskFiles.size)

    val input = taskB.taskFiles["${TEST_DATA_FOLDER}/1/${taskB.inputFileName}"]?.text
                ?: error("Can't find input file for test")
    val output = taskB.taskFiles["${TEST_DATA_FOLDER}/1/${taskB.outputFileName}"]?.text
                 ?: error("Can't find output file for test")

    assertEquals(expectedTest.input, input)
    assertEquals(expectedTest.output, output)
  }

  companion object {
    private data class TestSample(
      val input: String,
      val output: String
    )
  }
}