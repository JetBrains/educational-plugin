package com.jetbrains.edu.yaml.actions

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.actions.placeholder.CCEditAnswerPlaceholder
import com.jetbrains.edu.coursecreator.yaml.createConfigFiles
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.testAction
import org.junit.Test

class YamlActionsHelperTest : EduActionTestCase() {
  @Test
  fun `test navigate to yaml`() {
    val firstPlaceholderText = "TODO() Placeholder 1"
    val secondPlaceholderText = "TODO() Placeholder 2"

    val taskContents = """
      fun foo(): String = <p>$firstPlaceholderText</p>
      fun bar(): String = <p>$secondPlaceholderText</p>
    """.trimIndent()
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", taskContents)
        }
      }
    }
    createConfigFiles(project)
    val taskFile = findFile("lesson1/task1/Task.kt")

    val contentsWithoutPlaceholders = taskContents.replace(Regex("</?p>"), "")

    val firstPlaceholderOffset = contentsWithoutPlaceholders.indexOf(firstPlaceholderText)
    val secondPlaceholderOffset = contentsWithoutPlaceholders.indexOf(secondPlaceholderText)

    doTestEditPlaceholder(taskFile, firstPlaceholderOffset, firstPlaceholderText)
    doTestEditPlaceholder(taskFile, secondPlaceholderOffset, secondPlaceholderText)
  }

  private fun doTestEditPlaceholder(taskFile: VirtualFile, placeholderOffset: Int, placeholderText: String) {
    myFixture.openFileInEditor(taskFile)
    myFixture.editor.caretModel.moveToOffset(placeholderOffset)

    testAction(CCEditAnswerPlaceholder.ACTION_ID)

    val editor = FileEditorManagerEx.getInstanceEx(project).selectedTextEditor ?: error("editor should not be null")
    val textFromCaret = editor.document.text.substring(editor.caretModel.offset)
    assertTrue("Caret should be placed just before the placeholder text $placeholderText", textFromCaret.startsWith(placeholderText))
  }
}