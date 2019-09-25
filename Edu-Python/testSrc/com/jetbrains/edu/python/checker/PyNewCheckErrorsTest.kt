package com.jetbrains.edu.python.checker

import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckResultDiff
import com.jetbrains.edu.learning.checker.CheckResultDiffMatcher
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.nullValue
import com.jetbrains.python.PythonLanguage
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Assert

@Suppress("PyInterpreter")
class PyNewCheckErrorsTest : PyCheckersTestBase() {

  override fun createCourse(): Course {
    val test = """import unittest
              |from ..task import sum
              |class TestCase(unittest.TestCase):
              |    def test_add(self):
              |        self.assertEqual(sum(1, 2), 3, msg="error")""".trimMargin()

    return course(language = PythonLanguage.INSTANCE, environment = "unittest") {
      lesson {
        eduTask("EduTestsFailed") {
          pythonTaskFile("task.py", """def sum(a, b):
            |    return a + b + 1""".trimMargin())
          dir("tests") {
            taskFile("__init__.py")
            taskFile("tests.py", test)
          }
        }
        eduTask("EduNoTestsRun") {
          pythonTaskFile("task.py", """def sum(a, b):
            |    return a + b""".trimMargin())
          dir("tests") {
            taskFile("__init__.py")
            taskFile("tests.py")
          }
        }
        eduTask("SyntaxError") {
          pythonTaskFile("task.py", """def sum(a, b):
            |    return a + b hello goodbye""".trimMargin())
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
        "EduTestsFailed" -> Result(CheckStatus.Failed, CoreMatchers.equalTo("error\n3 != 4\n"),
                                   CheckResultDiffMatcher.diff(
                                     CheckResultDiff(expected = "4", actual = "3", title = "Comparison Failure (test_add)",
                                                     message = "error\n3 != 4\n")), nullValue())
        "EduNoTestsRun" -> Result(CheckStatus.Unchecked, CoreMatchers.containsString("No tests have run"), nullValue(), nullValue())
        "SyntaxError" -> Result(CheckStatus.Failed, CoreMatchers.containsString("Syntax Error"), nullValue(),
                                CoreMatchers.containsString("SyntaxError: invalid syntax"))
        "OutputTestsFailed" -> Result(CheckStatus.Failed, CoreMatchers.equalTo("Expected output:\n" +
                                                                               "Hello, World!\n" +
                                                                               " \n" +
                                                                               "Actual output:\n" +
                                                                               "Hello, World\n"),
                                      CheckResultDiffMatcher.diff(CheckResultDiff(expected = "Hello, World!\n", actual = "Hello, World\n")),
                                      nullValue())
        else -> error("Unexpected task name: ${task.name}")
      }

      assertEquals("Status for ${task.name} doesn't match", matcher.status, checkResult.status)
      Assert.assertThat("Checker message for ${task.name} doesn't match", checkResult.message, matcher.message)
      Assert.assertThat("Checker diff for ${task.name} doesn't match", checkResult.diff, matcher.diff)
      Assert.assertThat("Checker details for ${task.name} doesn't match", checkResult.details, matcher.details)
    }
    doTest()
  }

  data class Result(val status: CheckStatus,
                    val message: Matcher<String>,
                    val diff: Matcher<CheckResultDiff?>,
                    val details: Matcher<String?>)
}
