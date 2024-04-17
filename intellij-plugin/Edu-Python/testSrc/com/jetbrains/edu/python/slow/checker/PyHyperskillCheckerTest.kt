package com.jetbrains.edu.python.slow.checker

import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import com.jetbrains.python.PythonLanguage
import org.junit.Test

@Suppress("PyInterpreter", "PyUnresolvedReferences")
class PyHyperskillCheckerTest : PyCheckersTestBase() {

  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    // Hyperskill python support is not available in Android Studio
    if (!EduUtilsKt.isAndroidStudio()) {
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

  @Test
  fun testPythonCourse() {
    CheckActionListener.expectedMessage { CheckUtils.CONGRATULATIONS }
    doTest()
  }
}
