package com.jetbrains.edu.python

import com.jetbrains.edu.learning.*
import com.jetbrains.edu.python.learning.PyCourseBuilder
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.newProject.PyNewProjectSettings
import com.jetbrains.python.newProject.PythonProjectGenerator

class PyCourseBuilderTest : CourseGenerationTestBase<PyNewProjectSettings>() {

  override val courseBuilder: EduCourseBuilder<PyNewProjectSettings> = PyCourseBuilder()
  override val defaultSettings: PyNewProjectSettings = PythonProjectGenerator.NO_SETTINGS

  fun `test study course structure`() {
    generateCourseStructure("testData/newCourse/python_course.json")
    val expectedFileTree = fileTree {
      dir(".idea") {}
      dir("lesson1") {
        dir("task1") {
          file("hello_world.py")
          file("tests.py")
        }
        dir("task2") {
          file("comments.py")
          file("tests.py")
        }
      }
      dir("lesson2") {
        dir("task1") {
          file("variable_definition.py")
          file("tests.py")
        }
      }
      file("test_helper.py")
    }

    expectedFileTree.assertEquals(rootDir)
  }

  fun `test new course structure`() {
    val course = newCourse(PythonLanguage.INSTANCE)
    createCourseStructure(courseBuilder, course, defaultSettings)

    val expectedFileTree = fileTree {
      dir(".idea") {}
      dir("lesson1") {
        dir("task1") {
          file("task.py")
          file("tests.py")
          file("task.html")
        }
      }
      file("test_helper.py")
    }

    expectedFileTree.assertEquals(rootDir)
  }

  fun `test educator course structure from not empty course`() {
    generateCourseStructure("testData/newCourse/python_course.json", CourseType.EDUCATOR)
    val expectedFileTree = fileTree {
      dir(".idea") {}
      dir("lesson1") {
        dir("task1") {
          file("hello_world.py")
          file("tests.py")
          file("task.html")
        }
        dir("task2") {
          file("comments.py")
          file("tests.py")
          file("task.html")
        }
      }
      dir("lesson2") {
        dir("task1") {
          file("variable_definition.py")
          file("tests.py")
          file("task.html")
        }
      }
      file("test_helper.py")
    }

    expectedFileTree.assertEquals(rootDir)
  }
}
