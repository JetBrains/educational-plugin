package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.MapDataContext
import com.jetbrains.edu.coursecreator.CCTestCase
import com.jetbrains.edu.coursecreator.CCTestCase.getPlaceholders
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.taskFile.CCShowPreview
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduTestDialog
import com.jetbrains.edu.learning.TaskBuilder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.withEduTestDialog
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
    testAction(createDataContext(file), CCShowPreview())
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
    course.description = "my summary"
    val placeholder = course.lessons.first().taskList.first().taskFiles["fizz.kt"]!!.answerPlaceholders?.firstOrNull()
                      ?: error("Cannot find placeholder")
    placeholder.offset = 1000

    withEduTestDialog(EduTestDialog()) {
      testAction(createDataContext(findFile("lesson1/task1/fizz.kt")), CCShowPreview())
    }.checkWasShown(EduCoreBundle.message("exception.message.placeholder.info.single", 1000, 0))
  }

  fun `test show error if we have no placeholders`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("fizz.kt", "no placeholders")
        }
      }
    }
    course.description = "my summary"

    withEduTestDialog(EduTestDialog()) {
      testAction(createDataContext(findFile("lesson1/task1/fizz.kt")), CCShowPreview())
    }.checkWasShown(EduCoreBundle.message("dialog.message.no.preview.for.file"))
  }

  private fun doTest(taskFilePath: String, buildTask: TaskBuilder.() -> Unit) {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1", buildTask = buildTask)
      }
    }

    testAction(createDataContext(findFile(taskFilePath)), CCShowPreview())
    val editor = EditorFactory.getInstance().allEditors[0]

    try {
      val (document, placeholders) = getPlaceholders("$name.txt")
      assertEquals("Files don't match", document.text, editor.document.text)
      for (placeholder in placeholders) {
        CCTestCase.checkPainters(placeholder)
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

  private fun createDataContext(file: VirtualFile): DataContext {
    val context = MapDataContext()
    context.put(CommonDataKeys.PSI_FILE, PsiManager.getInstance(project).findFile(file))
    context.put(CommonDataKeys.PROJECT, project)
    context.put(LangDataKeys.MODULE, myFixture.module)
    return context
  }
}
