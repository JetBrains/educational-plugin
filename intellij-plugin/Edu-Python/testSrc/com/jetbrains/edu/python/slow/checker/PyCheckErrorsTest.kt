package com.jetbrains.edu.python.slow.checker

import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil.setDirectoryProjectSdk
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckResultDiffMatcher
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckResultDiff
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.messages.EduFormatBundle
import com.jetbrains.edu.learning.nullValue
import com.jetbrains.python.PythonLanguage
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

@Suppress("PyInterpreter", "PyUnresolvedReferences")
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
          /**
           * We do not have test_helper.py here because it is generated automatically
           * @see com.jetbrains.edu.python.learning.newproject.PyCourseProjectGenerator.createAdditionalFiles
           * */
          @Suppress("PyStatementEffect")
          pythonTaskFile("hello_world.py", """This is not Python code""")
          pythonTaskFile("tests.py", """
            from test_helper import run_common_tests, failed, passed, get_answer_placeholders
            run_common_tests()
            """)
        }
        eduTask("SyntaxErrorFromUnittest") {
          @Suppress("PyStatementEffect")
          pythonTaskFile("hello_world.py", """This is not Python code""")
          pythonTaskFile("my_unit_tests.py", """
            import unittest
            from hello_world import sum_of_two_digits

            class TestSumOfTwoDigits(unittest.TestCase):
                def test_something(self):
                    self.assertTrue(sum_of_two_digits())

            if __name__ == '__main__':
                unittest.main()
            """)
          pythonTaskFile("tests.py", """
            from test_helper import run_common_tests, failed, passed, import_file
            from unittest import defaultTestLoader, TestResult
            
            module = import_file("my_unit_tests.py")
            test_suite = defaultTestLoader.loadTestsFromModule(module)
            test_result = TestResult()
            test_suite.run(test_result)
            
            if test_result.wasSuccessful():
                passed()
            else:
                failed("Some unit tests failed")
            """)
        }
        eduTask("DoNotEscapeMessageInFailedTest") {
          pythonTaskFile("tests.py", """print("#educational_plugin test_type_used FAILED + <br>")""")
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

  @Test
  fun `test errors`() {
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      assertEquals("Status for ${task.name} doesn't match", CheckStatus.Failed, checkResult.status)
      val matcher: Triple<Matcher<String>, Matcher<CheckResultDiff?>, Matcher<String?>> = when (task.name) {
        "EduTestsFailed" -> Triple(equalTo("error happened"), nullValue(), nullValue())
        "EduNoTestsRun" -> Triple(containsString(EduFormatBundle.message("check.no.tests")), nullValue(), nullValue())
        "SyntaxError" -> Triple(containsString("Syntax Error"), nullValue(),
                                containsString("SyntaxError: invalid syntax"))
        "SyntaxErrorFromUnittest" -> Triple(containsString("Syntax Error"), nullValue(),
                                            containsString("SyntaxError: invalid syntax"))
        "DoNotEscapeMessageInFailedTest" -> Triple(equalTo("<br>"), nullValue(), nullValue())
        "OutputTestsFailed" ->
          Triple(equalTo(EduCoreBundle.message("check.incorrect")),
                 CheckResultDiffMatcher.diff(CheckResultDiff(expected = "Hello, World!\n", actual = "Hello, World\n")), nullValue())
        else -> error("Unexpected task name: ${task.name}")
      }
      assertThat("Checker output for ${task.name} doesn't match", checkResult.message, matcher.first)
      assertThat("Checker diff for ${task.name} doesn't match", checkResult.diff, matcher.second)
      assertThat("Checker output for ${task.name} doesn't match", checkResult.details, matcher.third)
    }
    doTest()
  }

  @Test
  fun `test no interpreter`() {
    setDirectoryProjectSdk(project, null)

    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      assertEquals("Status for ${task.name} doesn't match", CheckStatus.Unchecked, checkResult.status)
      assertThat("Checker output for ${task.name} doesn't match", checkResult.message,
                 containsString(EduCoreBundle.message("error.no.interpreter", EduFormatNames.PYTHON)))
    }
    doTest()
  }
}
