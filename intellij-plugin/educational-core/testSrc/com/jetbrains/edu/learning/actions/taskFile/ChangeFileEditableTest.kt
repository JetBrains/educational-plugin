package com.jetbrains.edu.learning.actions.taskFile

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.ReadOnlyModificationException
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.document
import org.junit.Test

class ChangeFileEditableTest : EduTestCase() {

  @Test
  fun `test modify non-editable files for study course`() {
    val initialTextInNonEditableFile = "text in nonEditableFile"
    val initialTextInEditableFile = "text in editableFile"

    courseWithFiles {
      frameworkLesson {
        eduTask {
          taskFile("nonEditableFile.txt", text = initialTextInNonEditableFile, editable = false)
          taskFile("editableFile.txt", text = initialTextInEditableFile)
        }
      }
    }

    checkEditabilityOfFile(path = "lesson1/task/nonEditableFile.txt",
                           initialTextInTask = initialTextInNonEditableFile,
                           mustBeEditable = false)

    checkEditabilityOfFile(path = "lesson1/task/editableFile.txt",
                           initialTextInTask = initialTextInEditableFile,
                           mustBeEditable = true)
  }


  @Test
  fun `test check editability of any file in task for educator course`() {
    val initialTextInNonEditableFile = "text in nonEditableFile"
    val initialTextInEditableFile = "text in editableFile"

    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task") {
          taskFile("nonEditableFile.txt", text = initialTextInNonEditableFile, editable = false)
          taskFile("editableFile.txt", text = initialTextInEditableFile)
        }
      }
    }

    checkEditabilityOfFile(path = "lesson1/task/nonEditableFile.txt",
                           initialTextInTask = initialTextInNonEditableFile,
                           mustBeEditable = true)
    checkEditabilityOfFile(path = "lesson1/task/editableFile.txt",
                           initialTextInTask = initialTextInEditableFile,
                           mustBeEditable = true)
  }

  private fun checkEditabilityOfFile(path: String, initialTextInTask: String, mustBeEditable: Boolean) {
    val file = findFile(path)
    assertEquals(initialTextInTask, file.document.text)
    val randomText = "random text"
    if (mustBeEditable) {
      runWriteAction { file.document.setText(randomText) }
      assertEquals(randomText, file.document.text)
    }
    else {
      assertThrows(ReadOnlyModificationException::class.java) {
        runWriteAction { file.document.setText(randomText) }
      }
      assertEquals(initialTextInTask, file.document.text)
    }
  }
}