package com.jetbrains.edu.rust.actions

import com.intellij.ide.actions.DeleteAction
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduTestDialog
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.withEduTestDialog
import com.jetbrains.edu.rust.RsProjectSettings
import org.intellij.lang.annotations.Language
import org.rust.lang.RsLanguage

class RsDeleteActionTest : RsActionTestBase() {

  fun `test delete first lesson`() = doTest(createRustCourse(), "lesson1", """
      [workspace]
      
      members = [
          "section1/lesson2/*/",
          "section1/lesson3/*/" ,
          "lesson4/*/"
      ]
      
      exclude = [
          "**/*.yaml"
      ]
  """)

  fun `test delete last lesson`() = doTest(createRustCourse(), "lesson4", """
      [workspace]
      
      members = [
          "lesson1/*/",
          "section1/lesson2/*/",
          "section1/lesson3/*/" ,
      ]
      
      exclude = [
          "**/*.yaml"
      ]
  """)

  fun `test delete last lesson with trailing comma`() {
    val course = courseWithFiles(
      courseMode = CCUtils.COURSE_MODE,
      language = RsLanguage,
      settings = RsProjectSettings()
    ) {
      lesson("lesson1") {
        eduTask("task1") {
          rustTaskFile("main.rs")
        }
      }
      lesson("lesson2") {
        eduTask("task2") {
          rustTaskFile("main.rs")
        }
      }
      additionalFile("Cargo.toml", """
        [workspace]
        
        members = [
            "lesson1/*/",
            "lesson2/*/",
        ]
        
        exclude = [
            "**/*.yaml"
        ]
      """.trimIndent())
    }

    doTest(course, "lesson2", """
        [workspace]
        
        members = [
            "lesson1/*/",
        ]
        
        exclude = [
            "**/*.yaml"
        ]
    """)
  }

  fun `test delete section`() = doTest(createRustCourse(), "section1", """
      [workspace]
      
      members = [
          "lesson1/*/",
          "lesson4/*/"
      ]
      
      exclude = [
          "**/*.yaml"
      ]
  """)

  fun `test delete last task in lesson`() = doTest(createRustCourse(), "lesson1/task1", """
      [workspace]
      
      members = [
          "section1/lesson2/*/",
          "section1/lesson3/*/" ,
          "lesson4/*/"
      ]
      
      exclude = [
          "**/*.yaml"
      ]
  """)

  fun `test delete non-last task in lesson`() = doTest(createRustCourse(), "lesson4/task5", """
      [workspace]
      
      members = [
          "lesson1/*/",
          "section1/lesson2/*/",
          "section1/lesson3/*/" ,
          "lesson4/*/"
      ]
      
      exclude = [
          "**/*.yaml"
      ]
  """)

  fun `test delete empty lesson`() = doTest(createRustCourse(), "lesson5", """
      [workspace]
      
      members = [
          "lesson1/*/",
          "section1/lesson2/*/",
          "section1/lesson3/*/" ,
          "lesson4/*/"
      ]
      
      exclude = [
          "**/*.yaml"
      ]
  """)

  private fun createRustCourse(): Course {
    return courseWithFiles(
      courseMode = CCUtils.COURSE_MODE,
      language = RsLanguage,
      settings = RsProjectSettings()
    ) {
      lesson("lesson1") {
        eduTask("task1") {
          rustTaskFile("main.rs")
        }
      }
      section("section1") {
        lesson("lesson2") {
          eduTask("task2") {
            rustTaskFile("main.rs")
          }
        }
        lesson("lesson3") {
          eduTask("task3") {
            rustTaskFile("main.rs")
          }
        }
      }
      lesson("lesson4") {
        eduTask("task4") {
          rustTaskFile("main.rs")
        }
        eduTask("task5") {
          rustTaskFile("main.rs")
        }
      }
      lesson("lesson5")
      additionalFile("Cargo.toml", """
        [workspace]
        
        members = [
            "lesson1/*/",
            "section1/lesson2/*/",
            "section1/lesson3/*/" ,
            "lesson4/*/"
        ]
        
        exclude = [
            "**/*.yaml"
        ]
      """.trimIndent())
    }
  }

  private fun doTest(course: Course, path: String, @Language("TOML") expectedText: String) {
    withVirtualFileListener(course) {
      withEduTestDialog(EduTestDialog()) {
        testAction(dataContext(findFile(path)), DeleteAction())
      }
    }
    checkCargoToml(expectedText)
  }
}
