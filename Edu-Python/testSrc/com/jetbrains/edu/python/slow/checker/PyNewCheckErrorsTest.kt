package com.jetbrains.edu.python.slow.checker

import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckResultDiff
import com.jetbrains.edu.learning.checker.CheckResultDiffMatcher.Companion.diff
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.nullValue
import com.jetbrains.python.PythonLanguage
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matcher
import org.junit.Assert.assertThat

@Suppress("PyInterpreter")
class PyNewCheckErrorsTest : PyCheckersTestBase() {

  override fun createCourse(): Course {
    val test = """
      |import unittest
      |from ..task import sum
      |class TestCase(unittest.TestCase):
      |    def test_add(self):
      |        self.assertEqual(sum(1, 2), 3, msg="error")
      |""".trimMargin()

    return course(language = PythonLanguage.INSTANCE, environment = "unittest") {
      lesson {
        eduTask("EduTestsFailed") {
          pythonTaskFile("task.py", """
            def sum(a, b):
                return a + b + 1
            """)
          dir("tests") {
            taskFile("__init__.py")
            taskFile("tests.py", test)
          }
        }
        eduTask("EduNoTestsRun") {
          pythonTaskFile("task.py", """
            def sum(a, b):
                return a + b
            """)
          dir("tests") {
            taskFile("__init__.py")
            taskFile("tests.py")
          }
        }
        eduTask("SyntaxError") {
          pythonTaskFile("task.py", """
            def sum(a, b):
                return a + b hello goodbye
            """)
          dir("tests") {
            taskFile("__init__.py")
            taskFile("tests.py", test)
          }
        }
        outputTask("OutputTestsFailed") {
          pythonTaskFile("hello_world.py", """print("Hello, World")""")
          taskFile("output.txt") {
            withText("Hello, World!\n")
          }
        }
      }
    }
  }

  fun `test errors`() {
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      val matcher = when (task.name) {
        "EduTestsFailed" -> Result(CheckStatus.Failed, equalTo("error\n3 != 4\n"),
                                   diff(CheckResultDiff(expected = "4", actual = "3", title = "Comparison Failure (test_add)",
                                                        message = "error\n3 != 4\n")), nullValue())
        "EduNoTestsRun" -> Result(CheckStatus.Unchecked, containsString("No tests have run"), nullValue(), nullValue())
        "SyntaxError" -> Result(CheckStatus.Failed, containsString("Syntax Error"), nullValue(),
                                containsString("SyntaxError: invalid syntax"))
        "OutputTestsFailed" -> Result(CheckStatus.Failed, equalTo("""
          |Expected output:
          |Hello, World!
          | 
          |Actual output:
          |Hello, World
          |""".trimMargin()), diff(CheckResultDiff(expected = "Hello, World!\n", actual = "Hello, World\n")), nullValue())
        else -> error("Unexpected task name: ${task.name}")
      }

      assertEquals("Status for ${task.name} doesn't match", matcher.status, checkResult.status)
      assertThat("Checker message for ${task.name} doesn't match", checkResult.message, matcher.message)
      assertThat("Checker diff for ${task.name} doesn't match", checkResult.diff, matcher.diff)
      assertThat("Checker details for ${task.name} doesn't match", checkResult.details, matcher.details)
    }
    doTest()
  }

  data class Result(
    val status: CheckStatus,
    val message: Matcher<String>,
    val diff: Matcher<CheckResultDiff?>,
    val details: Matcher<String?>
  )
}
