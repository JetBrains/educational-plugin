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
      dir("Introduction") {
        dir("Our first program") {
          file("hello_world.py")
          file("tests.py")
        }
        dir("Comments") {
          file("comments.py")
          file("tests.py")
        }
      }
      dir("Variables") {
        dir("Variable definition") {
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
      dir("lesson1") {
        dir("task1") {
          file("task.py")
          file("tests.py")
          file("task.html")
          file("task-info.yaml")
        }
        file("lesson-info.yaml")
      }
      file("test_helper.py")
      file("course-info.yaml")
    }

    expectedFileTree.assertEquals(rootDir)
  }

  fun `test educator course structure from not empty course`() {
    generateCourseStructure("testData/newCourse/python_course.json", CourseType.EDUCATOR)
    val expectedFileTree = fileTree {
      dir("Introduction") {
        dir("Our first program") {
          file("hello_world.py")
          file("tests.py")
          file("task.html")
          file("task-info.yaml")
        }
        dir("Comments") {
          file("comments.py")
          file("tests.py")
          file("task.html")
          file("task-info.yaml")
        }
        file("lesson-info.yaml")
      }
      dir("Variables") {
        dir("Variable definition") {
          file("variable_definition.py")
          file("tests.py")
          file("task.html")
          file("task-info.yaml")
        }
        file("lesson-info.yaml")
      }
      file("test_helper.py")
      file("course-info.yaml")
    }

    expectedFileTree.assertEquals(rootDir)
  }
}
