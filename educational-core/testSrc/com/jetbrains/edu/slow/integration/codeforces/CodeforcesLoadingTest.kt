package com.jetbrains.edu.slow.integration.codeforces

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.codeforces.CodeforcesTestCase
import com.jetbrains.edu.learning.codeforces.ContestParameters
import com.jetbrains.edu.learning.codeforces.StartCodeforcesContestAction
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask

class CodeforcesLoadingTest: CodeforcesTestCase() {
  private val contestKotlinHeroesEpisode1: CodeforcesCourse by lazy {
    val contestInfo = ContestParameters(1170, "en", "Kotlin", EduNames.KOTLIN)
    when (val contest = StartCodeforcesContestAction.getContestUnderProgress(contestInfo)) {
      is Err -> error(contest.error)
      is Ok -> contest.value
    }
  }

  private val codeforcesRound605Div3: CodeforcesCourse by lazy {
    val contestInfo = ContestParameters(1272, "en", "Java", EduNames.JAVA)
    when (val contest = StartCodeforcesContestAction.getContestUnderProgress(contestInfo)) {
      is Err -> error(contest.error)
      is Ok -> contest.value
    }
  }

  fun `test codeforces contest Kotlin Heroes Episode 1`() {
    val contest = contestKotlinHeroesEpisode1

    assertEquals(1170, contest.id)
    assertEquals("en", contest.languageCode)
    assertEquals("Kotlin Heroes: Episode 1", contest.name)
    assertEquals("https://codeforces.com/contest/1170", contest.getContestUrl())
    assertEquals("https://codeforces.com/contest/1170/submit?locale=en", contest.getSubmissionUrl())
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

    assertEquals("https://codeforces.com/contest/1170/problem/H?locale=en", tasks[7].feedbackLink.link)
  }

  fun `test codeforces contest Kotlin Heroes Episode 1 task A`() {
    val contest = contestKotlinHeroesEpisode1
    val task = contest.lessons.first().taskList[0] as CodeforcesTask

    assertEquals("A. Three Integers Again", task.name)
    assertEquals("https://codeforces.com/contest/1170/problem/A?locale=en", task.feedbackLink.link)
    task.checkTaskDescription(1170, 'A')
  }

  fun `test codeforces contest Kotlin Heroes Episode 1 task E with image`() {
    val contest = contestKotlinHeroesEpisode1
    val task = contest.lessons.first().taskList[4] as CodeforcesTask

    assertEquals("E. Sliding Doors", task.name)
    assertEquals("https://codeforces.com/contest/1170/problem/E?locale=en", task.feedbackLink.link)
    task.checkTaskDescription(1170, 'E')
  }

  fun `test codeforces contest Kotlin Heroes Episode 1 task G with image`() {
    val contest = contestKotlinHeroesEpisode1
    val task = contest.lessons.first().taskList[6] as CodeforcesTask

    assertEquals("G. Graph Decomposition", task.name)
    assertEquals("https://codeforces.com/contest/1170/problem/G?locale=en", task.feedbackLink.link)
    task.checkTaskDescription(1170, 'G')
  }

  fun `test codeforces contest Codeforces Round 605 (Div 3)`() {
    val contest = codeforcesRound605Div3

    assertEquals(1272, contest.id)
    assertEquals("en", contest.languageCode)
    assertEquals("Codeforces Round #605 (Div. 3)", contest.name)
    assertEquals("https://codeforces.com/contest/1272", contest.getContestUrl())
    assertEquals("https://codeforces.com/contest/1272/submit?locale=en", contest.getSubmissionUrl())
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

    assertEquals("https://codeforces.com/contest/1272/problem/D?locale=en", tasks[3].feedbackLink.link)
  }

  fun `test codeforces contest Codeforces Round 605 (Div 3) task A`() {
    val contest = codeforcesRound605Div3
    val task = contest.lessons.first().taskList[0] as CodeforcesTask

    assertEquals("A. Three Friends", task.name)
    assertEquals("https://codeforces.com/contest/1272/problem/A?locale=en", task.feedbackLink.link)
    task.checkTaskDescription(1272, 'A')
  }

  fun `test codeforces contest Codeforces Round 605 (Div 3) task B with image`() {
    val contest = codeforcesRound605Div3
    val task = contest.lessons.first().taskList[1] as CodeforcesTask

    assertEquals("B. Snow Walking Robot", task.name)
    assertEquals("https://codeforces.com/contest/1272/problem/B?locale=en", task.feedbackLink.link)
    task.checkTaskDescription(1272, 'B')
  }
}