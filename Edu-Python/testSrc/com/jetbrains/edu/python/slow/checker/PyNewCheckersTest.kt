package com.jetbrains.edu.python.slow.checker

import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.python.slow.checker.PyCheckersTestBase
import com.jetbrains.python.PythonLanguage

@Suppress("PyInterpreter")
class PyNewCheckersTest : PyCheckersTestBase() {

  override fun createCourse(): Course {
    return course(language = PythonLanguage.INSTANCE, environment = "unittest") {
      lesson {
        eduTask("Edu") {
          pythonTaskFile("task.py", """
            def sum(a, b):
                return a + b
            """)
          dir("tests") {
            taskFile("__init__.py")
            taskFile("tests.py", """
              import unittest
              from ..task import sum
              class TestCase(unittest.TestCase):
                  def test_add(self):
                      self.assertEqual(sum(1, 2), 3, msg="error")
              """)
          }
        }
        outputTask("Output") {
          pythonTaskFile("hello_world.py", """print("Hello, World!")""")
          taskFile("output.txt") {
            withText("Hello, World!\n")
          }
        }
      }
    }
  }

  fun `test python course`() {
    CheckActionListener.expectedMessage { task ->
      when (task) {
        is OutputTask, is EduTask -> CheckUtils.CONGRATULATIONS
        else -> null
      }
    }
    doTest()
  }
}
