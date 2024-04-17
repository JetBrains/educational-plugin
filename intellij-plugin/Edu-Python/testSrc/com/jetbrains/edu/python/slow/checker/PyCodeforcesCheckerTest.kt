package com.jetbrains.edu.python.slow.checker

import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckResultDiffMatcher
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_PROBLEMS
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.TEST_DATA_FOLDER
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckResultDiff
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesCourse
import com.jetbrains.edu.learning.nullValue
import com.jetbrains.python.PythonLanguage
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

@Suppress("PyInterpreter", "PyUnresolvedReferences")
class PyCodeforcesCheckerTest : PyCheckersTestBase() {
  override fun createCourse(): Course = course(language = PythonLanguage.INSTANCE, courseProducer = ::CodeforcesCourse) {
    lesson(CODEFORCES_PROBLEMS) {
      codeforcesTask("CodeforcesTask") {
        pythonTaskFile("src/main.py", """
          a = input()
          print(a)
        """)
        taskFile("${TEST_DATA_FOLDER}/1/input.txt") {
          withText("2\n")
        }
        taskFile("${TEST_DATA_FOLDER}/1/output.txt") {
          withText("2\n")
        }
      }
      codeforcesTask("WrongAnswerCodeforcesTask") {
        pythonTaskFile("src/main.py", """
          a = input()
          print(a)
        """)
        taskFile("${TEST_DATA_FOLDER}/1/input.txt") {
          withText("2\n")
        }
        taskFile("${TEST_DATA_FOLDER}/1/output.txt") {
          withText("3\n")
        }
      }
    }
  }

  @Test
  fun `test codeforces course`() {
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      val matcher = when (task.name) {
        "CodeforcesTask" -> CheckStatus.Solved to nullValue()
        "WrongAnswerCodeforcesTask" -> CheckStatus.Failed to CheckResultDiffMatcher.diff(CheckResultDiff(expected = "3", actual = "2"))
        else -> error("Unexpected task name: ${task.name}")
      }

      assertEquals("Status for ${task.name} doesn't match", matcher.first, checkResult.status)
      assertThat("Checker diff for ${task.name} doesn't match", checkResult.diff, matcher.second)
    }
    doTest()
  }
}
