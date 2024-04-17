package com.jetbrains.edu.kotlin.slow.checker

import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Test

class KtHyperskillCheckerTest : JdkCheckerTestBase() {
  override fun createCourse(): Course  {
    val course = course(courseProducer = ::HyperskillCourse, language = KotlinLanguage.INSTANCE) {
      frameworkLesson {
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
    } as HyperskillCourse
    course.stages = listOf(HyperskillStage(1, "", 1))
    course.hyperskillProject = HyperskillProject()
    return course
  }

  @Test
  fun testKotlinCourse() {
    CheckActionListener.expectedMessage { CheckUtils.CONGRATULATIONS }
    doTest()
  }
}
