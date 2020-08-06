package com.jetbrains.edu.python.slow.checker

import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStage
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.successMessage
import com.jetbrains.python.PythonLanguage

class PyHyperskillCheckerTest : PyCheckersTestBase() {

  override fun runTest() {
    if (!EduUtils.isAndroidStudio()) {
      super.runTest()
    }
  }

  override fun createCourse(): Course {
    val course = course(courseProducer = ::HyperskillCourse, language = PythonLanguage.INSTANCE) {
      frameworkLesson {
        eduTask("Edu") {
          pythonTaskFile("hello_world.py", """print("Hello, world! My name is type your name")""")
          pythonTaskFile("tests.py", """print("#educational_plugin test OK")""")
        }
      }
    } as HyperskillCourse
    course.stages = listOf(HyperskillStage(1, "", 1))
    course.hyperskillProject = HyperskillProject().apply { id = 42 }
    return course
  }

  fun testPythonCourse() {
    CheckActionListener.expectedMessage { it.successMessage }
    doTest()
  }
}