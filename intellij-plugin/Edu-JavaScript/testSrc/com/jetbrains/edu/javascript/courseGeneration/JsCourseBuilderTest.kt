package com.jetbrains.edu.javascript.courseGeneration

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.lang.javascript.ui.NodeModuleNamesUtil
import com.intellij.openapi.project.ProjectManager
import com.jetbrains.edu.javascript.learning.JsNewProjectSettings
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.newCourse
import org.junit.Test

class JsCourseBuilderTest : CourseGenerationTestBase<JsNewProjectSettings>() {

  override val defaultSettings = JsNewProjectSettings()
  override fun setUp() {
    super.setUp()
    val defaultProject = ProjectManager.getInstance().defaultProject
    val interpreterRef = NodeJsInterpreterManager.getInstance(defaultProject).interpreterRef
    defaultSettings.selectedInterpreter = interpreterRef.resolve(defaultProject)
  }

  @Test
  fun `test new educator course`() {
    val newCourse = newCourse(JavascriptLanguage.INSTANCE)

    createCourseStructure(newCourse)

    fileTree {
      dir("lesson1/task1") {
        file("task.js")
        dir("test") {
          file("test.js")
        }
        file("task.md")
      }
      file("package.json")
    }.assertEquals(rootDir)

    assertListOfAdditionalFiles(newCourse,
      "package.json" to getInternalTemplateText(NodeModuleNamesUtil.PACKAGE_JSON)
    )
  }

  @Test
  fun `test study course structure`() {
    val course = course(language = JavascriptLanguage.INSTANCE) {
      lesson {
        eduTask {
          taskFile("task.js")
          taskFile("test/test.js")
        }
      }
      additionalFiles {
        eduFile("package.json", "tmp")
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        file("task.js")
        dir("test") {
          file("test.js")
        }
        file("task.md")
      }
      file("package.json")
    }.assertEquals(rootDir)

    // package.json should not be overridden
    assertListOfAdditionalFiles(course,
      "package.json" to "tmp"
    )
  }

  @Test
  fun `recreate package_json for student`() {
    val newCourse = newCourse(JavascriptLanguage.INSTANCE, courseMode = CourseMode.STUDENT)

    createCourseStructure(newCourse)

    // package.json should not be created for a student
    assertListOfAdditionalFiles(newCourse)
  }
}
