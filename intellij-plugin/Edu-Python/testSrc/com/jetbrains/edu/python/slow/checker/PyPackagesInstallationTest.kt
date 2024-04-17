package com.jetbrains.edu.python.slow.checker

import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.python.PythonLanguage
import org.junit.Test

@Suppress("PyInterpreter", "PyUnresolvedReferences")
class PyPackagesInstallationTest : PyCheckersTestBase() {
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
              import requests
              from task import sum
              class TestCase(unittest.TestCase):
                  def test_add(self):
                      self.assertEqual(sum(1, 2), 3, msg="error")
              """)
          }
        }
      }
      additionalFile("requirements.txt", "requests")
    }

  }

  @Test
  fun `test required packages installed`() {
    CheckActionListener.expectedMessage { CheckUtils.CONGRATULATIONS }
    doTest()
  }
}