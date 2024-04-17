package com.jetbrains.edu.java.slow.checker

import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduTestInfo
import com.jetbrains.edu.learning.courseFormat.EduTestInfo.PresentableStatus.*
import org.junit.Test

class JExecutedTestsInfoTest : JdkCheckerTestBase() {
  override fun createCourse(): Course = course(language = JavaLanguage.INSTANCE) {
    lesson {
      eduTask("testFailed") {
        javaTaskFile("src/Task.java")
        javaTaskFile("test/Tests.java", """
            import org.junit.Assert;
            import org.junit.Test;
            
            public class Tests {
            
              @Test
              public void failedTest() {
                Assert.fail();
              }
            
              @Test
              public void failedTest2() {
                Assert.assertFalse(true);
              }
            }
          """
        )
      }
      eduTask("testIgnored") {
        javaTaskFile("src/Task.java")
        javaTaskFile("test/Tests.java", """
          import org.junit.Assert;
          import org.junit.Ignore;
          import org.junit.Test;
          
          public class Tests {
            @Ignore
            @Test
            public void ignoredTest() {
              Assert.assertTrue(false);
            }
            
            @Ignore
            @Test
            public void ignoredTest2() {
              Assert.assertTrue(true);
            }
          }
          """
        )
      }
      eduTask("testPassed") {
        javaTaskFile("src/Task.java")
        javaTaskFile("test/Tests.java", """
          import org.junit.Assert;
          import org.junit.Test;
          
          public class Tests {
            @Test
            public void passedTest() {
              Assert.assertTrue(true);
            }
            
            @Test
            public void passedTest2() {
              Assert.assertFalse(false);
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
        "testFailed" -> listOf(EduTestInfo("Test class Tests:failedTest", FAILED), EduTestInfo("Test class Tests:failedTest2", FAILED))
        "testIgnored" -> listOf(EduTestInfo("Test class Tests:ignoredTest", IGNORED), EduTestInfo("Test class Tests:ignoredTest2", IGNORED))
        "testPassed" -> listOf(EduTestInfo("Test class Tests:passedTest", COMPLETED), EduTestInfo("Test class Tests:passedTest2", COMPLETED))
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