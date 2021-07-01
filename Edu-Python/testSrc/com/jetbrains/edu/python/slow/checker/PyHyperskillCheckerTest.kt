package com.jetbrains.edu.python.slow.checker

import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStage
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.python.PythonLanguage

@Suppress("PyInterpreter", "PyUnresolvedReferences")
class PyHyperskillCheckerTest : PyCheckersTestBase() {

  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    // Hyperskill python support is not available in Android Studio
    if (!EduUtils.isAndroidStudio()) {
      super.runTestRunnable(context)
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
    course.hyperskillProject = HyperskillProject()
    return course
  }

  fun testPythonCourse() {
    CheckActionListener.expectedMessage { CheckUtils.CONGRATULATIONS }
    doTest()
  }
}
