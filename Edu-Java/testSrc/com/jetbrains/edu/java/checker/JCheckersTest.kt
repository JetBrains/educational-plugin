package com.jetbrains.edu.java.checker

import com.jetbrains.edu.java.JCourseBuilder
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckersTestBase
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.TestsOutputParser
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.intellij.JdkProjectSettings
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class JCheckersTest : CheckersTestBase() {

  override val dataPath: String = "checker"

  fun testJavaCourse() {
    CheckActionListener.expectedMessage { task -> if (task is EduTask) TestsOutputParser.CONGRATULATIONS else null }
    doTest()
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

  override fun getGenerator(course: Course): CourseProjectGenerator<JdkProjectSettings> =
          JCourseBuilder().getCourseProjectGenerator(course)
}
