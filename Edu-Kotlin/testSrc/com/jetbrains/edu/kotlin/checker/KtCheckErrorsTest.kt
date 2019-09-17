package com.jetbrains.edu.kotlin.checker

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckResultDiff
import com.jetbrains.edu.learning.checker.CheckResultDiffMatcher.Companion.diff
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.nullValue
import org.hamcrest.CoreMatchers.equalTo
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Assert.assertThat

class KtCheckErrorsTest : KtCheckersTestBase() {

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
              public static int i = aaa;
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
      outputTask("outputTaskFail") {
        kotlinTaskFile("src/Task.kt", """
          fun main(args: Array<String>) {
              println("OK")
          }
        """)
        taskFile("test/output.txt", "OK!")
      }
      outputTask("multilineOutputTaskFail") {
        kotlinTaskFile("src/Task.kt", """
          fun main(args: Array<String>) {
              println("Hello")
              println("World")
          }
        """)
        taskFile("test/output.txt", "Hello,\nWorld!")
      }
    }
  }

  fun testErrors() {
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      assertEquals("${task.name} should be failed", CheckStatus.Failed, checkResult.status)
      val (messageMatcher, diffMatcher) = when (task.name) {
        "kotlinCompilationError", "javaCompilationError", "compilationError()" ->
          equalTo(CheckUtils.COMPILATION_FAILED_MESSAGE) to nullValue()
        "testFail" ->
          equalTo("foo() should return 42") to nullValue()
        "comparisonTestFail" ->
          equalTo("expected:<42> but was:<43>") to
            diff(CheckResultDiff(expected = "42", actual = "43"))
        "comparisonTestWithMessageFail" ->
          equalTo("foo() should return 42 expected:<42> but was:<43>") to
            diff(CheckResultDiff(expected = "42", actual = "43", message = "foo() should return 42"))
        "comparisonMultilineTestFail" ->
          equalTo("Wrong Answer expected:<Hello[,]\nWorld!> but was:<Hello[]\nWorld!>") to
            diff(CheckResultDiff(expected = "Hello,\nWorld!", actual = "Hello\nWorld!", message="Wrong Answer"))
        "objectComparisonTestFail" ->
          // TODO: find out why test framework doesn't provide diff for this case
          equalTo("expected: Foo<(0, 0)> but was: Bar<(0, 0)>") to nullValue()
        "outputTaskFail" ->
          equalTo("Expected output:\n<OK!>\nActual output:\n<OK>") to
            diff(CheckResultDiff(expected = "OK!", actual = "OK"))
        "multilineOutputTaskFail" ->
          equalTo("Expected output:\n<Hello,\nWorld!>\nActual output:\n<Hello\nWorld>".trimIndent()) to
            diff(CheckResultDiff(expected = "Hello,\nWorld!", actual = "Hello\nWorld"))
        else -> error("Unexpected task `${task.name}`")
      }
      assertThat("Checker message for ${task.name} doesn't match", checkResult.message, messageMatcher)
      assertThat("Checker diff for ${task.name} doesn't match", checkResult.diff, diffMatcher)
    }
    doTest()
  }

  fun testBrokenJdk() {
    UIUtil.dispatchAllInvocationEvents()

    val jdk = SdkConfigurationUtil.setupSdk(arrayOfNulls(0), myProject.baseDir, JavaSdk.getInstance(), true, null, "Broken JDK")
    runWriteAction {
      ProjectRootManager.getInstance(myProject).projectSdk = jdk
      ProjectJdkTable.getInstance().addJdk(jdk!!)
    }

    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { CheckUtils.FAILED_TO_CHECK_MESSAGE }

    doTest()
  }
}
