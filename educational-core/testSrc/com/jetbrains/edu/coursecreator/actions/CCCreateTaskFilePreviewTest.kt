package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.coursecreator.CCTestsUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.taskFile.CCShowPreview
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.io.File

class CCCreateTaskFilePreviewTest : EduActionTestCase() {

  override fun getTestDataPath(): String = "${super.getTestDataPath()}/actions/preview"

  fun `test one placeholder`() = doTest("lesson1/task1/test.txt") {
    taskFile("test.txt", """<p>type here</p>""") {
      placeholder(0, possibleAnswer = "two")
    }
  }

  fun `test several placeholders`() = doTest("lesson1/task1/test.txt") {
    taskFile("test.txt", """print("<p>veryverylongtextveryverylongtextveryverylongtextveryverylongtext</p> + <p>test</p> = 2")""") {
      placeholder(0, possibleAnswer = "1")
      placeholder(1, possibleAnswer = "1")
    }
  }

  fun `test nested task file`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/Task.txt", """<p>type here</p>""") {
            placeholder(0, possibleAnswer = "answer")
          }
        }
      }
    }

    val file = findFile("lesson1/task1/src/Task.txt")
    testAction(dataContext(file), CCShowPreview.ACTION_ID)
    val editor = EditorFactory.getInstance().allEditors[0]
    try {
      val document = editor.document
      val virtualFile = FileDocumentManager.getInstance().getFile(document)!!
      assertEquals("Task.txt", virtualFile.name)
    }
    finally {
      EditorFactory.getInstance().releaseEditor(editor)
    }
  }

  fun `test show error if placeholder is broken`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("fizz.kt", """fn fizzz() = <p>TODO()</p>""")
        }
      }
    }
    val placeholder = course.lessons.first().taskList.first().taskFiles["fizz.kt"]!!.answerPlaceholders?.firstOrNull()
                      ?: error("Cannot find placeholder")
    placeholder.offset = 1000

    withEduTestDialog(EduTestDialog()) {
      testAction(dataContext(findFile("lesson1/task1/fizz.kt")), CCShowPreview.ACTION_ID)
    }.checkWasShown(EduCoreBundle.message("exception.broken.placeholder.message", "lesson1/task1/fizz.kt", 1000, 0))
  }

  fun `test show error if we have no placeholders`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("fizz.kt", "no placeholders")
        }
      }
    }

    withEduTestDialog(EduTestDialog()) {
      testAction(dataContext(findFile("lesson1/task1/fizz.kt")), CCShowPreview.ACTION_ID)
    }.checkWasShown(EduCoreBundle.message("dialog.message.no.preview.for.file"))
  }

  private fun doTest(taskFilePath: String, buildTask: TaskBuilder.() -> Unit) {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1", buildTask = buildTask)
      }
    }

    testAction(dataContext(findFile(taskFilePath)), CCShowPreview.ACTION_ID)
    val editor = EditorFactory.getInstance().allEditors[0]

    try {
      val (document, placeholders) = getPlaceholders("$name.txt")
      assertEquals("Files don't match", document.text, editor.document.text)
      for (placeholder in placeholders) {
        CCTestsUtil.checkPainters(placeholder)
      }
    }
    finally {
      EditorFactory.getInstance().releaseEditor(editor)
    }
  }

  private fun getPlaceholders(name: String): Pair<Document, List<AnswerPlaceholder>> {
    val text = StringUtil.convertLineSeparators(FileUtil.loadFile(File(testDataPath, name)))
    val tempDocument = EditorFactory.getInstance().createDocument(text)
    val placeholders = getPlaceholders(tempDocument, true)
    return tempDocument to placeholders
  }
}
