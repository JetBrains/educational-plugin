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
class PyCheckErrorsTest : PyCheckersTestBase() {

  override fun createCourse(): Course {
    return course(language = PythonLanguage.INSTANCE) {
      lesson {
        eduTask("EduTestsFailed") {
          pythonTaskFile("hello_world.py", """print("Hello, world! My name is type your name")""")
          pythonTaskFile("tests.py", """print("#educational_plugin test_type_used FAILED + error happened")""")
        }
        eduTask("EduNoTestsRun") {
          pythonTaskFile("hello_world.py", """print("Hello, world! My name is type your name")""")
          pythonTaskFile("tests.py", """print("")""")
        }
        eduTask("SyntaxError") {
          pythonTaskFile("hello_world.py", """This is not Python code""")
          pythonTaskFile("tests.py", """from test_helper import run_common_tests, failed, passed, get_answer_placeholders
            |run_common_tests()""".trimMargin())
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
      assertEquals(CheckStatus.Failed, checkResult.status)
      val matcher = when (task.name) {
        "EduTestsFailed" -> Result(CoreMatchers.containsString("error happened"), nullValue(), nullValue())
        "EduNoTestsRun" -> Result(CoreMatchers.containsString("No tests have run"), nullValue(), nullValue())
        "SyntaxError" -> Result(CoreMatchers.containsString("Syntax Error"), nullValue(),
                                CoreMatchers.containsString("SyntaxError: invalid syntax"))
        "OutputTestsFailed" ->
          Result(CoreMatchers.equalTo("Expected output:\n" +
                                      "Hello, World!\n" +
                                      " \n" +
                                      "Actual output:\n" +
                                      "Hello, World\n"),
                 CheckResultDiffMatcher.diff(CheckResultDiff(expected = "Hello, World!\n", actual = "Hello, World\n")), nullValue())
        else -> error("Unexpected task name: ${task.name}")
      }
      Assert.assertThat("Checker output for ${task.name} doesn't match", checkResult.message, matcher.message)
      Assert.assertThat("Checker diff for ${task.name} doesn't match", checkResult.diff, matcher.diff)
      Assert.assertThat("Checker output for ${task.name} doesn't match", checkResult.details, matcher.details)
    }
    doTest()
  }

  private data class Result(val message: Matcher<String>, val diff: Matcher<CheckResultDiff?>, val details: Matcher<String?>)
}
