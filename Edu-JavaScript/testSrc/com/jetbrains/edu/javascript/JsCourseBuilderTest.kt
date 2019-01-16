package com.jetbrains.edu.javascript

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.openapi.project.ProjectManager
import com.jetbrains.edu.javascript.learning.JsCourseBuilder
import com.jetbrains.edu.javascript.learning.JsNewProjectSettings
import com.jetbrains.edu.learning.*

class JsCourseBuilderTest : CourseGenerationTestBase<JsNewProjectSettings>() {

  override val courseBuilder = JsCourseBuilder()
  override val defaultSettings = JsNewProjectSettings()
  override fun setUp() {
    super.setUp()
    val defaultProject = ProjectManager.getInstance().defaultProject
    val interpreterRef = NodeJsInterpreterManager.getInstance(defaultProject).interpreterRef
    defaultSettings.selectedInterpreter = interpreterRef.resolve(defaultProject)
  }

  fun `test new educator course`() {
    val newCourse = newCourse(JavascriptLanguage.INSTANCE)

    createCourseStructure(newCourse)

    fileTree {
      dir("lesson1/task1") {
        file("task.js")
        dir("test") {
          file("test.js")
        }
        file("task.html")
      }
      file("package.json")
    }.assertEquals(rootDir)
  }

  fun `test study course structure`() {
    val course = course(language = JavascriptLanguage.INSTANCE) {
      lesson {
        eduTask {
          taskFile("task.js")
          taskFile("test/test.js")
        }
        lesson(EduNames.ADDITIONAL_MATERIALS) {
          eduTask(EduNames.ADDITIONAL_MATERIALS) {
            taskFile("package.json")
          }
        }
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        file("task.js")
        dir("test") {
          file("test.js")
        }
      }
      file("package.json")
    }.assertEquals(rootDir)
  }
}
