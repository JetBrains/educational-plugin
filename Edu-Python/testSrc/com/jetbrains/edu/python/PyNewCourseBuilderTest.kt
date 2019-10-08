package com.jetbrains.edu.python

import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.newProject.PyNewProjectSettings
import com.jetbrains.python.newProject.PythonProjectGenerator

class PyNewCourseBuilderTest : CourseGenerationTestBase<PyNewProjectSettings>() {
  override val defaultSettings: PyNewProjectSettings = PythonProjectGenerator.NO_SETTINGS

  fun `test new educator course`() {
    val course = pythonCourse(CourseMode.EDUCATOR)
    createCourseStructure(course)
    fileTree {
      dir("lesson1") {
        dir("task1") {
          file("__init__.py")
          file("task.py")
          file("task.html")
          dir("tests") {
            file("__init__.py")
            file("test_task.py")
          }
        }
      }
    }.assertEquals(rootDir)
  }

  fun `test student course`() {
    val course = pythonCourse(CourseMode.STUDENT) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("__init__.py")
          taskFile("file1.py")
          taskFile("file2.py")
          taskFile("tests/__init__.py")
          taskFile("tests/tests.py")
        }
        eduTask("task2") {
          taskFile("__init__.py")
          taskFile("file3.py")
          taskFile("file4.py")
          taskFile("tests/__init__.py")
          taskFile("tests/tests1.py")
          taskFile("tests/tests2.py")
        }
      }
      lesson("lesson2") {
        eduTask("task1") {
          taskFile("__init__.py")
          taskFile("file5.py")
          taskFile("file6.py")
          taskFile("tests/__init__.py")
          taskFile("tests/tests.py")
        }
      }
    }
    createCourseStructure(course)
    fileTree {
      dir("lesson1") {
        dir("task1") {
          file("__init__.py")
          file("file1.py")
          file("file2.py")
          file("task.html")
          dir("tests") {
            file("__init__.py")
            file("tests.py")
          }
        }
        dir("task2") {
          file("__init__.py")
          file("file3.py")
          file("file4.py")
          file("task.html")
          dir("tests") {
            file("__init__.py")
            file("tests1.py")
            file("tests2.py")
          }
        }
      }
      dir("lesson2") {
        dir("task1") {
          file("__init__.py")
          file("file5.py")
          file("file6.py")
          file("task.html")
          dir("tests") {
            file("__init__.py")
            file("tests.py")
          }
        }
      }
    }.assertEquals(rootDir)
  }

  private fun pythonCourse(courseMode: CourseMode, buildCourse: CourseBuilder.() -> Unit = {}): Course = course(
    language = PythonLanguage.INSTANCE,
    environment = "unittest",
    courseMode = courseMode.toString(),
    buildCourse = buildCourse
  )
}
