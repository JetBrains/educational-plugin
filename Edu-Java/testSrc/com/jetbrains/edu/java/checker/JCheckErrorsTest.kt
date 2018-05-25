package com.jetbrains.edu.java.checker

import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course

class JCheckErrorsTest : JCheckersTestBase() {

  override fun createCourse(): Course = course(language = JavaLanguage.INSTANCE) {
    lesson {
      eduTask("javaCompilationError") {
        javaTaskFile("Task.java", """
          public class Task {
            public static final String STRING;
          }
        """)
        javaTestFile("Test.java", """
            class Test {}
        """)
      }
      eduTask("testFail") {
        javaTaskFile("Task.java", """
          public class Task {
            public static int foo() {
              return 0;
            }
          }
        """)
        javaTestFile("Test.java", """
          import org.junit.Assert;

          public class Test {
            @org.junit.Test
            public void test() {
              Assert.assertTrue("Task.foo() should return 42", Task.foo() == 42);
            }
          }
        """)
      }
    }
  }

  fun testErrors() {
    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { task ->
      when (task.name) {
        "javaCompilationError" -> CheckUtils.COMPILATION_FAILED_MESSAGE
        "testFail" -> "Task.foo() should return 42"
        else -> null
      }
    }
    doTest()
  }
}
