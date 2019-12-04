package com.jetbrains.edu.python.slow.checker

import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckResultDiff
import com.jetbrains.edu.learning.checker.CheckResultDiffMatcher
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
          pythonTaskFile("tests.py", """
            from test_helper import run_common_tests, failed, passed, get_answer_placeholders
            run_common_tests()""")
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
      assertEquals("Status for ${task.name} doesn't match", CheckStatus.Failed, checkResult.status)
      val matcher: Triple<Matcher<String>, Matcher<CheckResultDiff?>, Matcher<String?>> = when (task.name) {
        "EduTestsFailed" -> Triple(containsString("error happened"), nullValue(), nullValue())
        "EduNoTestsRun" -> Triple(containsString("No tests have run"), nullValue(), nullValue())
        "SyntaxError" -> Triple(containsString("Syntax Error"), nullValue(),
                                containsString("SyntaxError: invalid syntax"))
        "OutputTestsFailed" ->
          Triple(equalTo("""
          |Expected output:
          |Hello, World!
          | 
          |Actual output:
          |Hello, World
          |""".trimMargin()),
                 CheckResultDiffMatcher.diff(CheckResultDiff(expected = "Hello, World!\n", actual = "Hello, World\n")), nullValue())
        else -> error("Unexpected task name: ${task.name}")
      }
      assertThat("Checker output for ${task.name} doesn't match", checkResult.message, matcher.first)
      assertThat("Checker diff for ${task.name} doesn't match", checkResult.diff, matcher.second)
      assertThat("Checker output for ${task.name} doesn't match", checkResult.details, matcher.third)
    }
    doTest()
  }
}
