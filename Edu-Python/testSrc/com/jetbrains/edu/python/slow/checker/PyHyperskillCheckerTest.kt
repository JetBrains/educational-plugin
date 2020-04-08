package com.jetbrains.edu.python.slow.checker

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.successMessage
import com.jetbrains.python.PythonLanguage

class PyHyperskillCheckerTest : PyCheckersTestBase() {

  override fun runTest() {
    if (!EduUtils.isAndroidStudio()) {
      super.runTest()
    }
  }

  override fun createCourse(): Course = course(courseProducer = ::HyperskillCourse, language = PythonLanguage.INSTANCE,
                                               courseMode = CCUtils.COURSE_MODE) {
    lesson {
      eduTask("Edu") {
        pythonTaskFile("hello_world.py", """print("Hello, world! My name is type your name")""")
        pythonTaskFile("tests.py", """print("#educational_plugin test OK")""")
      }
    }
  }

  fun testPythonCourse() {
    CheckActionListener.expectedMessage { it.successMessage }
    doTest()
  }
}