package com.jetbrains.edu.scala.slow.checker

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
import org.hamcrest.CoreMatchers.equalTo
import org.jetbrains.plugins.scala.ScalaLanguage
import org.junit.Assert

class ScalaGradleCheckErrorTest : JdkCheckerTestBase() {
  override fun createCourse(): Course {
    return course(language = ScalaLanguage.INSTANCE, environment = "Gradle") {
      lesson {
        eduTask("compilationError") {
          scalaTaskFile("src/Task.scala", """
            class Task {
              def foo(): Int
            }
          """)
          scalaTaskFile("test/TestSpec.scala", """
            import org.junit.runner.RunWith
            import org.scalatest.junit.JUnitRunner
            import org.scalatest.FunSuite
            
            @RunWith(classOf[JUnitRunner])
            class TestSpec extends FunSuite {
              test("Test") {
                assertResult(42) { new Task().foo() }
              }
            }
          """)
        }

        eduTask("testFail") {
          scalaTaskFile("src/Task.scala", """
            class Task {
              def foo(): Int = 42
            }
          """)
          scalaTaskFile("test/TestSpec.scala", """
            import org.junit.runner.RunWith
            import org.scalatest.junit.JUnitRunner
            import org.scalatest.FunSuite
            
            @RunWith(classOf[JUnitRunner])
            class TestSpec extends FunSuite {
              test("Test") {
                fail("Message")
              }
            }
          """)
        }

        eduTask("comparisonTestFail") {
          scalaTaskFile("src/Task.scala", """
            class Task {
              def foo(): Int = 43
            }
          """)
          scalaTaskFile("test/TestSpec.scala", """
            import org.junit.runner.RunWith
            import org.scalatest.junit.JUnitRunner
            import org.scalatest.FunSuite
            
            @RunWith(classOf[JUnitRunner])
            class TestSpec extends FunSuite {
              test("Test") {
                assertResult(42) { new Task().foo() }
              }
            }
          """)
        }

        outputTask("outputTaskFail") {
          scalaTaskFile("src/Main.scala", """
            object Main {
              def main(args: Array[String]): Unit = {
                println("OK")
              }
            }
          """)
          taskFile("test/output.txt") {
            withText("OK!\n")
          }
        }
      }
    }
  }

  fun `test scala errors`() {
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      assertEquals("${task.name} should be failed", CheckStatus.Failed, checkResult.status)
      val (messageMatcher, diffMatcher) = when (task.name) {
        "compilationError" -> equalTo(CheckUtils.COMPILATION_FAILED_MESSAGE) to nullValue()
        "testFail" -> equalTo("Message") to nullValue()
        "comparisonTestFail" ->
          equalTo("Expected 42, but got 43") to nullValue()
        "outputTaskFail" ->
          equalTo(EduCoreBundle.message("check.incorrect")) to
            diff(CheckResultDiff(expected = "OK!\n", actual = "OK\n"))
        else -> error("Unexpected task `${task.name}`")
      }
      Assert.assertThat("Checker message for ${task.name} doesn't match", checkResult.message, messageMatcher)
      Assert.assertThat("Checker diff for ${task.name} doesn't match", checkResult.diff, diffMatcher)
    }
    doTest()
  }
}
