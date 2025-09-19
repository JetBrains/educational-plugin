package com.jetbrains.edu.kotlin.slow.checker

import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckResultDiffMatcher.Companion.diff
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckResultDiff
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduTestInfo
import com.jetbrains.edu.learning.courseFormat.EduTestInfo.PresentableStatus.FAILED
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.nullValue
import com.jetbrains.edu.learning.xmlEscaped
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Test

@Suppress("NonFinalUtilityClass")
class KtCheckErrorsTest : JdkCheckerTestBase() {

  override fun createCourse(): Course = course(language = KotlinLanguage.INSTANCE) {
    lesson {
      eduTask("kotlinCompilationError") {
        kotlinTaskFile("src/Task.kt", "fun foo(): Int = aaa")
        kotlinTaskFile("test/Tests.kt", """
          import org.junit.Assert
          import org.junit.Test

          class Test {
              @Test
              fun testSolution() {
                  Assert.assertTrue("foo() should return 42", foo() == 42)
              }
          }
        """)
      }
      eduTask("javaCompilationError") {
        javaTaskFile("src/JavaClass.java", """
          public class JavaClass {
              public static final int i = aaa;
          }
        """)
        kotlinTaskFile("src/Task.kt", """
          fun foo() = JavaClass.i
        """)
        kotlinTaskFile("test/Tests.kt", """
          import org.junit.Assert
          import org.junit.Test

          class Test {
              @Test
              fun testSolution() {
                  Assert.assertTrue("foo() should return 42", foo() == 42)
              }
          }
        """)
      }
      // handle case when task module contains `(` or `)`
      eduTask("compilationError()") {
        kotlinTaskFile("src/Task.kt", "fun foo(): Int = aaa")
        kotlinTaskFile("test/Tests.kt", """
          import org.junit.Assert
          import org.junit.Test

          class Test {
              @Test
              fun testSolution() {
                  Assert.assertTrue("foo() should return 42", foo() == 42)
              }
          }
        """)
      }
      eduTask("testFail") {
        kotlinTaskFile("src/Task.kt", """
          fun foo(): Int = 43
        """)
        kotlinTaskFile("test/Tests.kt", """
          import org.junit.Assert
          import org.junit.Test

          class Test {
              @Test
              fun testSolution() {
                  Assert.assertTrue("foo() should return 42", foo() == 42)
              }
          }
        """)
      }
      eduTask("comparisonTestFail") {
        kotlinTaskFile("src/Task.kt", """
          fun foo(): Int = 43
        """)
        kotlinTaskFile("test/Tests.kt", """
          import org.junit.Assert
          import org.junit.Test

          class Test {
              @Test
              fun testSolution() {
                  Assert.assertEquals(42, foo())
              }
          }
        """)
      }
      eduTask("comparisonTestWithMessageFail") {
        kotlinTaskFile("src/Task.kt", """
          fun foo(): Int = 43
        """)
        kotlinTaskFile("test/Tests.kt", """
          import org.junit.Assert
          import org.junit.Test

          class Test {
              @Test
              fun testSolution() {
                  Assert.assertEquals("foo() should return 42", 42, foo())
              }
          }
        """)
      }
      eduTask("comparisonMultilineTestFail") {
        kotlinTaskFile("src/Task.kt", """
          fun foo(): String = "Hello\nWorld!"
        """)
        kotlinTaskFile("test/Tests.kt", """
          import org.junit.Assert
          import org.junit.Test

          class Test {
              @Test
              fun testSolution() {
                  Assert.assertEquals("Wrong Answer", "Hello,\nWorld!", foo())
              }
          }
        """)
      }
      eduTask("objectComparisonTestFail") {
        kotlinTaskFile("src/Task.kt", $$"""
          data class Foo(val x: Int, val y: Int) {
              override fun toString(): String = "($x, $y)"
          }

          data class Bar(val x: Int, val y: Int) {
              override fun toString(): String = "($x, $y)"
          }
        """)
        kotlinTaskFile("test/Tests.kt", """
          import org.junit.Assert
          import org.junit.Test

          class Test {
              @Test fun testSolution() {
                  Assert.assertEquals(Foo(0, 0), Bar(0, 0))
              }
          }
        """)
      }
      eduTask("escapeMessageInFailedTest") {
        kotlinTaskFile("src/Task.kt")
        kotlinTaskFile("test/Tests.kt", """
          import org.junit.Assert
          import org.junit.Test

          class Test {
              @Test
              fun testSolution() {
                  Assert.assertTrue("<br>", false)
              }
          }
        """)
      }
      eduTask("gradleCustomRunConfiguration") {
        @Suppress("RedundantNullableReturnType")
        kotlinTaskFile("src/Task.kt", """
          fun foo(): String? = System.getenv("EXAMPLE_ENV")
        """)
        kotlinTaskFile("test/Tests.kt", """
          import org.junit.Assert
          import org.junit.Test
          
          class Tests {
              @Test
              fun fail() {
                  Assert.fail()
              }
          
              @Test
              fun testSolution() {
                  Assert.assertEquals("Hello", foo())
              }
          }
        """)
        dir("runConfigurations") {
          xmlTaskFile("CustomGradleCheck.run.xml", $$"""
            <component name="ProjectRunConfigurationManager">
              <configuration default="false" name="CustomGradleCheck" type="GradleRunConfiguration" factoryName="Gradle">
                <ExternalSystemSettings>
                  <option name="env">
                    <map>
                      <entry key="EXAMPLE_ENV" value="Hello!" />
                    </map>
                  </option>                
                  <option name="executionName" />
                  <option name="externalProjectPath" value="$PROJECT_DIR$" />
                  <option name="externalSystemIdString" value="GRADLE" />
                  <option name="scriptParameters" value="" />
                  <option name="taskDescriptions">
                    <list />
                  </option>
                  <option name="taskNames">
                    <list>
                      <option value=":$TASK_GRADLE_PROJECT$:test" />
                      <option value="--tests" />
                      <option value="&quot;Tests.testSolution&quot;" />
                    </list>
                  </option>
                  <option name="vmOptions" value="" />
                </ExternalSystemSettings>
                <ExternalSystemDebugServerProcess>false</ExternalSystemDebugServerProcess>
                <ExternalSystemReattachDebugProcess>true</ExternalSystemReattachDebugProcess>
                <DebugAllEnabled>false</DebugAllEnabled>
                <method v="2" />
              </configuration>
            </component>
          """)
        }
      }
      outputTask("outputTaskFail") {
        kotlinTaskFile("src/Task.kt", """
          fun main() {
              println("OK")
          }
        """)
        taskFile("test/output.txt") {
          withText("OK!\n")
        }
      }
      outputTask("outputTaskWithNewLineFail") {
        kotlinTaskFile("src/Task.kt", """
          fun main() {
              print("Line")
          }
        """)
        taskFile("test/output.txt") {
          withText("Line\n")
        }
      }
      outputTask("multilineOutputTaskFail") {
        kotlinTaskFile("src/Task.kt", """
          fun main() {
              println("Hello")
              println("World")
          }
        """)
        taskFile("test/output.txt") {
          withText("Hello,\nWorld!\n")
        }
      }
    }
  }

  @Test
  fun `test errors`() {
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      assertEquals("${task.name} should be failed", CheckStatus.Failed, checkResult.status)
      val title = "Comparison Failure (testSolution)"
      val testComparisonData = when (task.name) {
        "kotlinCompilationError", "javaCompilationError", "compilationError()" -> TestComparisonData(
          equalTo(CheckUtils.COMPILATION_FAILED_MESSAGE),
          nullValue()
        )

        "testFail" -> TestComparisonData(
          equalTo("foo() should return 42"), nullValue(), listOf(EduTestInfo("Test class Test:testSolution", FAILED))
        )

        "comparisonTestFail" -> TestComparisonData(
          equalTo(EduCoreBundle.message("check.incorrect")),
          diff(CheckResultDiff(expected = "42", actual = "43", title = title)),
          listOf(EduTestInfo("Test class Test:testSolution", FAILED))
        )

        "comparisonTestWithMessageFail" -> TestComparisonData(
          equalTo("foo() should return 42"),
          diff(CheckResultDiff(expected = "42", actual = "43", title = title)),
          listOf(EduTestInfo("Test class Test:testSolution", FAILED))
        )

        "comparisonMultilineTestFail" -> TestComparisonData(
          equalTo("Wrong Answer"),
          diff(CheckResultDiff(expected = "Hello,\nWorld!", actual = "Hello\nWorld!", title = title)),
          listOf(EduTestInfo("Test class Test:testSolution", FAILED))
        )

        "objectComparisonTestFail" ->
          // TODO: find out why test framework doesn't provide diff for this case
          TestComparisonData(
            equalTo("expected: Foo<(0, 0)> but was: Bar<(0, 0)>".xmlEscaped),
            nullValue(),
            listOf(EduTestInfo("Test class Test:testSolution", FAILED))
          )

        "escapeMessageInFailedTest" -> TestComparisonData(
          equalTo("<br>".xmlEscaped),
          nullValue(),
          listOf(EduTestInfo("Test class Test:testSolution", FAILED))
        )

        "gradleCustomRunConfiguration" -> TestComparisonData(
          equalTo(EduCoreBundle.message("check.incorrect")),
          diff(CheckResultDiff(expected = "Hello", actual = "Hello!", title = title)),
          listOf(EduTestInfo("Test class Tests:testSolution", FAILED))
        )

        "outputTaskFail" -> TestComparisonData(
          equalTo(EduCoreBundle.message("check.incorrect")), diff(CheckResultDiff(expected = "OK!\n", actual = "OK\n"))
        )

        "outputTaskWithNewLineFail" -> TestComparisonData(
          equalTo(EduCoreBundle.message("check.incorrect")),
          diff(CheckResultDiff(expected = "Line\n", actual = "Line"))
        )

        "multilineOutputTaskFail" -> TestComparisonData(
          equalTo(EduCoreBundle.message("check.incorrect")), diff(CheckResultDiff(expected = "Hello,\nWorld!\n", actual = "Hello\nWorld\n"))
        )

        else -> error("Unexpected task `${task.name}`")
      }
      assertThat("Checker message for ${task.name} doesn't match", checkResult.message, testComparisonData.messageMatcher)
      assertThat("Checker diff for ${task.name} doesn't match", checkResult.diff, testComparisonData.diffMatcher)
      assertEquals(
        "Number of executed tests for ${task.name} is wrong", testComparisonData.executedTestsInfo.size, checkResult.executedTestsInfo.size
      )
      testComparisonData.executedTestsInfo.forEach { testInfo ->
        val actualTestInfo = checkResult.executedTestsInfo.find { it.name == testInfo.name } ?: error(
          "Expected test ${testInfo.name} of ${task.name} task wasn't found " + "in test results: ${checkResult.executedTestsInfo}"
        )
        assertEquals(
          "Status of test from ${task.name} task is wrong", testInfo.toString(), actualTestInfo.toString()
        )
      }
    }
    doTest()
  }
}
