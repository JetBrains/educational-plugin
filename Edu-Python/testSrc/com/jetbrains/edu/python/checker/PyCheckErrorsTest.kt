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
import org.junit.Assert

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
      val (messageMatcher, diffMatcher) = when (task.name) {
        "EduTestsFailed" -> CoreMatchers.containsString("error happened") to nullValue()
        "EduNoTestsRun" -> CoreMatchers.containsString("No tests have run") to nullValue()
        "OutputTestsFailed" ->
          CoreMatchers.equalTo("Expected output:\n" +
                               "Hello, World!\n" +
                               " \n" +
                               "Actual output:\n" +
                               "Hello, World\n") to
            CheckResultDiffMatcher.diff(CheckResultDiff(expected = "Hello, World!\n", actual = "Hello, World\n"))
        else -> error("Unexpected task name: ${task.name}")
      }
      Assert.assertThat("Checker output for ${task.name} doesn't match", checkResult.message, messageMatcher)
      Assert.assertThat("Checker diff for ${task.name} doesn't match", checkResult.diff, diffMatcher)
    }
    doTest()
  }
}
