package com.jetbrains.edu.kotlin.slow.checker

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.hyperskill.SUCCESS_MESSAGE
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import org.jetbrains.kotlin.idea.KotlinLanguage

class KtHyperskillCheckerTest : JdkCheckerTestBase() {

  override fun createCourse(): Course = course(courseProducer = ::HyperskillCourse, language = KotlinLanguage.INSTANCE,
                                               courseMode = CCUtils.COURSE_MODE) {
    lesson {
      eduTask("EduTask") {
        kotlinTaskFile("src/Task.kt", """
          fun foo() = 42
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
    }
  }

  fun testKotlinCourse() {
    CheckActionListener.expectedMessage { SUCCESS_MESSAGE }
    doTest()
  }
}
