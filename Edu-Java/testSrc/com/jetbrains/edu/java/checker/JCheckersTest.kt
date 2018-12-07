package com.jetbrains.edu.java.checker

import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.TestsOutputParser
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

class JCheckersTest : JCheckersTestBase() {

  override fun createCourse(): Course = course(language = JavaLanguage.INSTANCE) {
    lesson {
      eduTask("EduTask") {
        javaTaskFile("src/Task.java", """
          public class Task {
            public static int foo() {
              return 42;
            }
          }
        """)
        javaTaskFile("test/Test.java", """
          import org.junit.Assert;

          public class Test {
            @org.junit.Test
            public void test() {
              Assert.assertTrue("Task.foo() should return 42", Task.foo() == 42);
            }
          }
        """)
      }
      theoryTask("TheoryTask") {
        javaTaskFile("src/Task.java", """
          public class Task {
            public static void main(String[] args) {
              System.out.println("OK");
            }
          }
        """)
      }
      outputTask("OutputTask") {
        javaTaskFile("src/Task.java", """
          public class Task {
            public static void main(String[] args) {
              System.out.println("OK");
            }
          }
        """)
        taskFile("test/output.txt", "OK")
      }
      outputTask("OutputTaskWithSeveralFiles") {
        javaTaskFile("src/Utils.java", """
          public class Utils {
            public static String ok() {
              return "OK";
            }
          }
        """)
        javaTaskFile("src/Task.java", """
          public class Task {
            public static void main(String[] args) {
              System.out.println(Utils.ok());
            }
          }
        """)
        taskFile("test/output.txt", "OK")
      }
    }
  }

  fun testJavaCourse() {
    CheckActionListener.expectedMessage { task ->
      when (task) {
        is OutputTask, is EduTask -> TestsOutputParser.CONGRATULATIONS
        is TheoryTask -> ""
        else -> null
      }
    }
    doTest()
  }
}
