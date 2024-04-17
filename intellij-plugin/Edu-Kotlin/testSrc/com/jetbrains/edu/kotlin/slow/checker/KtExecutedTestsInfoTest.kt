package com.jetbrains.edu.kotlin.slow.checker

import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduTestInfo
import com.jetbrains.edu.learning.courseFormat.EduTestInfo.PresentableStatus.*
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Test

class KtExecutedTestsInfoTest : JdkCheckerTestBase() {
  override fun createCourse(): Course = course(language = KotlinLanguage.INSTANCE) {
    lesson {
      eduTask("testFailed") {
        kotlinTaskFile("src/Task.kt")
        kotlinTaskFile("test/Tests.kt", """
          import org.junit.Assert
          import org.junit.Test
          
          class Test {
            @Test
            fun failedTest() {
              Assert.assertTrue(false)
            }
          
            @Test
            fun failedTest2() {
              Assert.assertFalse(true)
            }
          }
          """
        )
      }
      eduTask("testIgnored") {
        kotlinTaskFile("src/Task.kt")
        kotlinTaskFile("test/Tests.kt", """
          import org.junit.Assert
          import org.junit.Ignore
          import org.junit.Test
          
          class Test {
            @Ignore
            @Test
            fun ignoredTest() {
              Assert.assertTrue(false)
            }
          
            @Ignore
            @Test
            fun ignoredTest2() {
              Assert.assertTrue(true)
            }
          }
          """
        )
      }
      eduTask("testPassed") {
        kotlinTaskFile("src/Task.kt")
        kotlinTaskFile("test/Tests.kt", """
          import org.junit.Assert
          import org.junit.Test
          
          class Test {
            @Test
            fun passedTest() {
              Assert.assertTrue(true)
            }
          
            @Test
            fun passedTest2() {
              Assert.assertFalse(false)
            }
          }
          """
        )
      }
    }
  }

  @Test
  fun `test executed tests info`() {
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      val expectedTestsInfo = when (task.name) {
        "testFailed" -> listOf(EduTestInfo("Test class Test:failedTest", FAILED), EduTestInfo("Test class Test:failedTest2", FAILED))
        "testIgnored" -> listOf(EduTestInfo("Test class Test:ignoredTest", IGNORED), EduTestInfo("Test class Test:ignoredTest2", IGNORED))
        "testPassed" -> listOf(EduTestInfo("Test class Test:passedTest", COMPLETED), EduTestInfo("Test class Test:passedTest2", COMPLETED))
        else -> error("Unexpected task `${task.name}`")
      }
      assertEquals(
        "Number of executed tests for ${task.name} is wrong", expectedTestsInfo.size, checkResult.executedTestsInfo.size
      )
      expectedTestsInfo.forEach { testInfo ->
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