package com.jetbrains.edu.kotlin.slow.checker

import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckResultDiff
import com.jetbrains.edu.learning.checker.CheckResultDiffMatcher.Companion.diff
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.nullValue
import com.jetbrains.edu.learning.xmlEscaped
import org.hamcrest.CoreMatchers.equalTo
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Assert.assertThat

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
        kotlinTaskFile("src/Task.kt", """
          data class Foo(val x: Int, val y: Int) {
              override fun toString(): String = "(${'$'}x, ${'$'}y)"
          }

          data class Bar(val x: Int, val y: Int) {
              override fun toString(): String = "(${'$'}x, ${'$'}y)"
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

  fun `test errors`() {
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      assertEquals("${task.name} should be failed", CheckStatus.Failed, checkResult.status)
      val title = "Comparison Failure (testSolution)"
      val (messageMatcher, diffMatcher) = when (task.name) {
        "kotlinCompilationError", "javaCompilationError", "compilationError()" ->
          equalTo(CheckUtils.COMPILATION_FAILED_MESSAGE) to nullValue()
        "testFail" ->
          equalTo("foo() should return 42") to nullValue()
        "comparisonTestFail" ->
          equalTo(EduCoreBundle.message("check.incorrect")) to
            diff(CheckResultDiff(expected = "42", actual = "43", title = title))
        "comparisonTestWithMessageFail" ->
          equalTo("foo() should return 42") to
            diff(CheckResultDiff(expected = "42", actual = "43", title = title))
        "comparisonMultilineTestFail" ->
          equalTo("Wrong Answer") to
            diff(CheckResultDiff(expected = "Hello,\nWorld!", actual = "Hello\nWorld!", title = title))
        "objectComparisonTestFail" ->
          // TODO: find out why test framework doesn't provide diff for this case
          equalTo("expected: Foo<(0, 0)> but was: Bar<(0, 0)>".xmlEscaped) to nullValue()
        "escapeMessageInFailedTest" ->
          equalTo("<br>".xmlEscaped) to nullValue()
        "outputTaskFail" ->
          equalTo(EduCoreBundle.message("check.incorrect")) to
            diff(CheckResultDiff(expected = "OK!\n", actual = "OK\n"))
        "outputTaskWithNewLineFail" ->
          equalTo(EduCoreBundle.message("check.incorrect")) to
            diff(CheckResultDiff(expected = "Line\n", actual = "Line"))
        "multilineOutputTaskFail" ->
          equalTo(EduCoreBundle.message("check.incorrect")) to
            diff(CheckResultDiff(expected = "Hello,\nWorld!\n", actual = "Hello\nWorld\n"))
        else -> error("Unexpected task `${task.name}`")
      }
      assertThat("Checker message for ${task.name} doesn't match", checkResult.message, messageMatcher)
      assertThat("Checker diff for ${task.name} doesn't match", checkResult.diff, diffMatcher)
    }
    doTest()
  }
}
