package com.jetbrains.edu.rust.actions

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.actions.create.MockNewStudyItemUi
import com.jetbrains.edu.coursecreator.actions.studyItem.CCCreateLesson
import com.jetbrains.edu.coursecreator.actions.studyItem.CCCreateTask
import com.jetbrains.edu.coursecreator.ui.withMockCreateStudyItemUi
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.testAction
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.rust.cargo.CargoConstants
import org.rust.lang.RsLanguage

class RsCreateLessonTest : RsActionTestBase() {

  @Test
  fun `test add lesson item no trailing comma`() = addLastLesson("""
    [workspace]

    members = [
        "lesson1/*/"
    ]

    exclude = [
        "**/*.yaml"
    ]    
  """, """
    [workspace]
    
    members = [
        "lesson1/*/",
        "lesson2/*/",
    ]

    exclude = [
        "**/*.yaml"
    ]    
  """)

  @Test
  fun `test add lesson item trailing comma`() = addLastLesson("""
    [workspace]

    members = [
        "lesson1/*/",
    ]

    exclude = [
        "**/*.yaml"
    ]
  """, """
    [workspace]
    
    members = [
        "lesson1/*/",
        "lesson2/*/",
    ]
    
    exclude = [
        "**/*.yaml"
    ]
  """)

  @Test
  fun `test add lesson comments`() = addLastLesson("""
    [workspace]

    members = [
        "lesson1/*/" # very useful comment
    ]

    exclude = [
        "**/*.yaml"
    ]
  """, """
    [workspace]
    
    members = [
        "lesson1/*/", # very useful comment
        "lesson2/*/",
    ]
    
    exclude = [
        "**/*.yaml"
    ]
  """)

  @Test
  fun `test add lesson extra spaces`() = addLastLesson("""
    [workspace]

    members = [
        "lesson1/*/" ,
    ]

    exclude = [
        "**/*.yaml"
    ]
  """, """
    [workspace]
    
    members = [
        "lesson1/*/" ,
        "lesson2/*/",
    ]
    
    exclude = [
        "**/*.yaml"
    ]
  """)

  @Test
  fun `test add lesson item with section`() {
    courseWithFiles(
      courseMode = CourseMode.EDUCATOR,
      language = RsLanguage
    ) {
      section("section1") {
        lesson("lesson1") {
          eduTask("task1")
        }
      }
      additionalFile(CargoConstants.MANIFEST_FILE, """
        [workspace]

        members = [
            "section1/lesson1/*/",
        ]

        exclude = [
            "**/*.yaml"
        ]
      """.trimIndent())
    }

    addNewLessonWithTask("lesson2", "task1", findFile("section1"))

    checkCargoToml("""
      [workspace]
      
      members = [
          "section1/lesson1/*/",
          "section1/lesson2/*/",
      ]
      
      exclude = [
          "**/*.yaml"
      ]
    """)
  }

  @Test
  fun `test add the first lesson in course`() {
    courseWithFiles(
      courseMode = CourseMode.EDUCATOR,
      language = RsLanguage
    ) {
      section("section1")
      additionalFile("Cargo.toml", """
        [workspace]

        members = [
        ]

        exclude = [
            "**/*.yaml"
        ]
      """.trimIndent())
    }

    addNewLessonWithTask("lesson1", "task1", findFile("section1"))

    checkCargoToml("""
      [workspace]
      
      members = [
          "section1/lesson1/*/",
      ]
      
      exclude = [
          "**/*.yaml"
      ]
    """)
  }

  @Test
  fun `test add lesson in the middle of course`() {
    courseWithFiles(
      courseMode = CourseMode.EDUCATOR,
      language = RsLanguage
    ) {
      lesson("lesson1") {
        eduTask("task1")
      }
      lesson("lesson3") {
        eduTask("task3")
      }
      additionalFile("Cargo.toml", """
        [workspace]

        members = [
            "lesson1/*/",
            "lesson3/*/"
        ]

        exclude = [
            "**/*.yaml"
        ]
      """.trimIndent())
    }

    addNewLessonWithTask("lesson2", "task2", findFile("lesson1"))

    checkCargoToml("""
      [workspace]

      members = [
          "lesson1/*/",
          "lesson2/*/",
          "lesson3/*/"
      ]

      exclude = [
          "**/*.yaml"
      ]
    """)
  }

  @Test
  fun `test do not modify manifest on empty lesson creation`() {
    courseWithFiles(
      courseMode = CourseMode.EDUCATOR,
      language = RsLanguage
    ) {
      lesson("lesson1") {
        eduTask("task1")
      }
      additionalFile("Cargo.toml", """
        [workspace]

        members = [
            "lesson1/*/",
        ]

        exclude = [
            "**/*.yaml"
        ]
      """.trimIndent())
    }

    withMockCreateStudyItemUi(MockNewStudyItemUi("lesson2")) {
      testAction(CCCreateLesson.ACTION_ID, dataContext(LightPlatformTestCase.getSourceRoot()))
    }

    checkCargoToml("""
        [workspace]

        members = [
            "lesson1/*/",
        ]

        exclude = [
            "**/*.yaml"
        ]
      """)
  }

  @Test
  fun `test do not modify manifest on non first task creation`() {
    courseWithFiles(
      courseMode = CourseMode.EDUCATOR,
      language = RsLanguage
    ) {
      lesson("lesson1") {
        eduTask("task1")
      }
      additionalFile("Cargo.toml", """
        [workspace]

        members = [
            "lesson1/*/",
        ]

        exclude = [
            "**/*.yaml"
        ]
      """.trimIndent())
    }

    withMockCreateStudyItemUi(MockNewStudyItemUi("task2")) {
      testAction(CCCreateTask.ACTION_ID, dataContext(findFile("lesson1")))
    }

    checkCargoToml("""
        [workspace]

        members = [
            "lesson1/*/",
        ]

        exclude = [
            "**/*.yaml"
        ]
      """)
  }


  private fun addLastLesson(@Language("TOML") before: String, @Language("TOML") after: String) {
    courseWithFiles(
      courseMode = CourseMode.EDUCATOR,
      language = RsLanguage
    ) {
      lesson("lesson1") {
        eduTask("task1")
      }
      additionalFile(CargoConstants.MANIFEST_FILE, before.trimIndent())
    }

    addNewLessonWithTask("lesson2", "task2", LightPlatformTestCase.getSourceRoot())

    checkCargoToml(after)
  }

  private fun addNewLessonWithTask(lessonName: String, taskName: String, context: VirtualFile) {
    withMockCreateStudyItemUi(MockNewStudyItemUi(lessonName)) {
      testAction(CCCreateLesson.ACTION_ID, dataContext(context))
    }

    val lessonDir = context.findChild(lessonName) ?: context.parent.findChild(lessonName)
    check(lessonDir != null)
    withMockCreateStudyItemUi(MockNewStudyItemUi(taskName)) {
      testAction(CCCreateTask.ACTION_ID, dataContext(lessonDir))
    }
  }
}
