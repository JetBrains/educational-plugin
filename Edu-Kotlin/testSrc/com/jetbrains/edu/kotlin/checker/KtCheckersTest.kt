package com.jetbrains.edu.kotlin.checker

import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.TestsOutputParser
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import org.jetbrains.kotlin.idea.KotlinLanguage

class KtCheckersTest : KtCheckersTestBase() {

  override fun createCourse(): Course = course(language = KotlinLanguage.INSTANCE) {
    lesson {
      eduTask("EduTask") {
        kotlinTaskFile("Task.kt", """
          fun foo() = 42
        """)
        kotlinTestFile("Tests.kt", """
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
      theoryTask("TheoryTask") {
        kotlinTaskFile("Task.kt", """
          fun main(args: Array<String>) {
              val a = 1
              println(a)
          }
        """)
      }
      outputTask("OutputTask") {
        kotlinTaskFile("Task.kt", """
          fun main(args: Array<String>) {
              println("OK")
            }
        """)
        testFile("output.txt", "OK")
      }
    }
  }

  fun testKotlinCourse() {
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
