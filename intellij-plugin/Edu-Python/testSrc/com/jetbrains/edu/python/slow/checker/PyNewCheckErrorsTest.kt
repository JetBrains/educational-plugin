package com.jetbrains.edu.python.slow.checker

import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckResultDiffMatcher.Companion.diff
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckResultDiff
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PYTHON
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
class PyNewCheckErrorsTest : PyCheckersTestBase() {

  override fun createCourse(): Course {
    val test = """
      |import unittest
      |from task import sum
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
        eduTask("AssertionError") {
          pythonTaskFile("task.py")
          dir("tests") {
            taskFile("__init__.py")
            taskFile("tests.py", """
              |import unittest
              |class TestCase(unittest.TestCase):
              |    def test_add(self):
              |        self.assertTrue(False, "My own message")
              |""".trimMargin())
          }
        }
        eduTask("DoNotEscapeMessageInFailedTest") {
          pythonTaskFile("task.py")
          dir("tests") {
            taskFile("__init__.py")
            taskFile("tests.py", """
              |import unittest
              |class TestCase(unittest.TestCase):
              |    def test_add(self):
              |        self.assertTrue(False, "<br>")
              |""".trimMargin())
          }
        }
        eduTask("CustomRunConfiguration") {
          pythonTaskFile("task.py", """
            import os


            def hello():
                return os.getenv("EXAMPLE_ENV")
            """)
          dir("tests") {
            taskFile("__init__.py")
            taskFile("tests.py", """
              import unittest

              from task import hello


              class TestCase(unittest.TestCase):
                  def test_hello(self):
                      self.assertEqual(hello(), "Hello", msg="Error message")
              """)
          }
          dir("runConfigurations") {
            xmlTaskFile("CustomCheck.run.xml", $$"""
              <component name="ProjectRunConfigurationManager">
                <configuration name="CustomCheck" type="tests" factoryName="Unittests">
                  <module name="Python Course14" />
                  <option name="INTERPRETER_OPTIONS" value="" />
                  <option name="PARENT_ENVS" value="true" />
                  <envs>
                    <env name="EXAMPLE_ENV" value="Hello!" />
                  </envs>
                  <option name="SDK_HOME" value="$PROJECT_DIR$/.idea/VirtualEnvironment/bin/python" />
                  <option name="WORKING_DIRECTORY" value="$TASK_DIR$" />
                  <option name="IS_MODULE_SDK" value="true" />
                  <option name="ADD_CONTENT_ROOTS" value="true" />
                  <option name="ADD_SOURCE_ROOTS" value="true" />
                  <EXTENSION ID="PythonCoverageRunConfigurationExtension" runner="coverage.py" />
                  <option name="_new_pattern" value="&quot;&quot;" />
                  <option name="_new_additionalArguments" value="&quot;&quot;" />
                  <option name="_new_target" value="&quot;tests.TestCase&quot;" />
                  <option name="_new_targetType" value="&quot;PYTHON&quot;" />
                  <method v="2" />
                </configuration>
              </component>
            """)
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

  @Test
  fun `test errors`() {
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      val matcher = when (task.name) {
        "EduTestsFailed" ->
          Result(CheckStatus.Failed, equalTo("4 != 3 : error"),
                 diff(CheckResultDiff(expected = "4", actual = "3", title = "Comparison Failure (test_add)")), nullValue())
        "EduNoTestsRun" -> Result(CheckStatus.Unchecked, containsString(EduFormatBundle.message("check.no.tests")), nullValue(), nullValue())
        "SyntaxError" -> Result(CheckStatus.Failed, containsString("Syntax Error"), nullValue(),
                                containsString("SyntaxError: invalid syntax"))
        "AssertionError" -> Result(CheckStatus.Failed, equalTo("False is not true : My own message"), nullValue(), nullValue())
        "DoNotEscapeMessageInFailedTest" -> Result(CheckStatus.Failed, equalTo("False is not true : <br>"),
                                                   nullValue(), nullValue())
        "CustomRunConfiguration" ->
          Result(CheckStatus.Failed, equalTo("'Hello!' != 'Hello'"),
                 diff(CheckResultDiff(expected = "Hello!", actual = "Hello", title = "Comparison Failure (test_hello)")), nullValue())

        "OutputTestsFailed" ->
          Result(CheckStatus.Failed, equalTo(EduCoreBundle.message("check.incorrect")),
                 diff(CheckResultDiff(expected = "Hello, World!\n", actual = "Hello, World\n")), nullValue())
        else -> error("Unexpected task name: ${task.name}")
      }

      assertEquals("Status for ${task.name} doesn't match", matcher.status, checkResult.status)
      assertThat("Checker message for ${task.name} doesn't match", checkResult.message, matcher.message)
      assertThat("Checker diff for ${task.name} doesn't match", checkResult.diff, matcher.diff)
      assertThat("Checker details for ${task.name} doesn't match", checkResult.details, matcher.details)
    }
    doTest()
  }

  @Test
  fun `test no interpreter`() {
    SdkConfigurationUtil.setDirectoryProjectSdk(project, null)

    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      assertEquals("Status for ${task.name} doesn't match", CheckStatus.Unchecked, checkResult.status)
      assertThat("Checker output for ${task.name} doesn't match", checkResult.message,
                 containsString(EduCoreBundle.message("error.no.interpreter", PYTHON)))
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
