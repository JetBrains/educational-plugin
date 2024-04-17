package com.jetbrains.edu.learning

import com.intellij.find.FindModel
import com.intellij.find.impl.FindInProjectUtil
import com.intellij.find.replaceInProject.ReplaceInProjectManager
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.psi.PsiDocumentManager
import com.intellij.usageView.UsageInfo
import com.intellij.usages.FindUsagesProcessPresentation
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.util.CommonProcessors
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import org.junit.Test
import java.io.IOException

class EduDocumentListenerTest : EduTestCase() {

  @Test
  fun `test type in placeholder`() {
    val lessonIndex = 1
    val taskIndex = 1
    val taskFileName = "taskFile1.txt"
    configureByTaskFile(lessonIndex, taskIndex, taskFileName)
    myFixture.editor.caretModel.moveToOffset(13)
    myFixture.editor.selectionModel.removeSelection()
    myFixture.type("test")
    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson$lessonIndex")
    val task = lesson!!.getTask("task$taskIndex")
    val taskFile = task!!.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(1, answerPlaceholders.size)
    assertEquals("itests", answerPlaceholders[0].currentText)
  }

  @Test
  fun `test type before placeholder`() {
    val lessonIndex = 1
    val taskIndex = 1
    val taskFileName = "taskFile1.txt"
    configureByTaskFile(lessonIndex, taskIndex, taskFileName)
    myFixture.editor.caretModel.moveToOffset(1)
    myFixture.editor.selectionModel.removeSelection()
    myFixture.type("test")
    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson$lessonIndex")
    val task = lesson!!.getTask("task$taskIndex")
    val taskFile = task!!.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(1, answerPlaceholders.size)
    assertEquals("is", answerPlaceholders[0].currentText)
  }

  @Test
  fun `test type between placeholder`() {
    val lessonIndex = 1
    val taskIndex = 2
    val taskFileName = "taskFile2.txt"
    configureByTaskFile(lessonIndex, taskIndex, taskFileName)
    myFixture.editor.caretModel.moveToOffset(16)
    myFixture.editor.selectionModel.removeSelection()
    myFixture.type("test")
    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson$lessonIndex")
    val task = lesson!!.getTask("task$taskIndex")
    val taskFile = task!!.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(2, answerPlaceholders.size)
    assertEquals("is", answerPlaceholders[0].currentText)
    assertEquals("is another", answerPlaceholders[1].currentText)
  }

  @Test
  fun `test type in second placeholder`() {
    val lessonIndex = 1
    val taskIndex = 2
    val taskFileName = "taskFile2.txt"
    configureByTaskFile(lessonIndex, taskIndex, taskFileName)
    myFixture.editor.caretModel.moveToOffset(40)
    myFixture.editor.selectionModel.removeSelection()
    myFixture.type("test")
    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson$lessonIndex")
    val task = lesson!!.getTask("task$taskIndex")
    val taskFile = task!!.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(2, answerPlaceholders.size)
    assertEquals("is", answerPlaceholders[0].currentText)
    assertEquals("testis another", answerPlaceholders[1].currentText)
  }

  @Test
  fun `test type in first placeholder`() {
    val lessonIndex = 1
    val taskIndex = 2
    val taskFileName = "taskFile2.txt"
    configureByTaskFile(lessonIndex, taskIndex, taskFileName)
    myFixture.editor.caretModel.moveToOffset(12)
    myFixture.editor.selectionModel.removeSelection()
    myFixture.type("test")
    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson$lessonIndex")
    val task = lesson!!.getTask("task$taskIndex")
    val taskFile = task!!.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(2, answerPlaceholders.size)
    assertEquals("testis", answerPlaceholders[0].currentText)
    assertEquals("is another", answerPlaceholders[1].currentText)
  }

  @Test
  fun `test type after last placeholder`() {
    val lessonIndex = 1
    val taskIndex = 2
    val taskFileName = "taskFile2.txt"
    configureByTaskFile(lessonIndex, taskIndex, taskFileName)
    myFixture.editor.caretModel.moveToOffset(55)
    myFixture.editor.selectionModel.removeSelection()
    myFixture.type("test")
    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson$lessonIndex")
    val task = lesson!!.getTask("task$taskIndex")
    val taskFile = task!!.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(2, answerPlaceholders.size)
    assertEquals("is", answerPlaceholders[0].currentText)
    assertEquals("is another", answerPlaceholders[1].currentText)

  }

  @Test
  fun `test new line at the beginning placeholder`() {
    val lessonIndex = 1
    val taskIndex = 1
    val taskFileName = "taskFile1.txt"
    configureByTaskFile(lessonIndex, taskIndex, taskFileName)
    myFixture.editor.caretModel.moveToOffset(13)
    myFixture.editor.selectionModel.removeSelection()
    myFixture.type("\n")
    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson$lessonIndex")
    val task = lesson!!.getTask("task$taskIndex")
    val taskFile = task!!.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(1, answerPlaceholders.size)
    assertEquals("i\ns", answerPlaceholders[0].currentText)
  }

  @Test
  fun `test delete before placeholder`() {
    val taskFileName = "taskFile2.txt"
    configureByTaskFile(1, 2, taskFileName)

    runUndoTransparentWriteAction {
      myFixture.getDocument(myFixture.file).deleteString(5, 11)
    }

    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson1")
    val task = lesson!!.getTask("task2")
    val taskFile = task!!.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(2, answerPlaceholders.size)
    assertEquals("is", answerPlaceholders[0].currentText)
    assertEquals("is another", answerPlaceholders[1].currentText)
  }

  @Test
  fun `test delete between placeholder`() {
    val taskFileName = "taskFile2.txt"
    configureByTaskFile(1, 2, taskFileName)

    runUndoTransparentWriteAction {
      myFixture.getDocument(myFixture.file).deleteString(33, 39)
    }

    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson1")
    val task = lesson!!.getTask("task2")
    val taskFile = task!!.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(2, answerPlaceholders.size)
    assertEquals("is", answerPlaceholders[0].currentText)
    assertEquals("is another", answerPlaceholders[1].currentText)
  }

  @Test
  fun `test delete after placeholder`() {
    val taskFileName = "taskFile2.txt"
    configureByTaskFile(1, 2, taskFileName)

    runUndoTransparentWriteAction {
      myFixture.getDocument(myFixture.file).deleteString(55, 58)
    }

    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson1")
    val task = lesson!!.getTask("task2")
    val taskFile = task!!.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(2, answerPlaceholders.size)
    assertEquals("is", answerPlaceholders[0].currentText)
    assertEquals("is another", answerPlaceholders[1].currentText)
  }

  @Test
  fun `test delete first placeholder start`() {
    val taskFileName = "taskFile2.txt"
    configureByTaskFile(1, 2, taskFileName)

    runUndoTransparentWriteAction {
      myFixture.getDocument(myFixture.file).deleteString(11, 13)
    }

    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson1")
    val task = lesson!!.getTask("task2")
    val taskFile = task!!.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(2, answerPlaceholders.size)
    assertEquals("s", answerPlaceholders[0].currentText)
    assertEquals("is another", answerPlaceholders[1].currentText)
  }

  @Test
  fun `test delete first placeholder end`() {
    val taskFileName = "taskFile2.txt"
    configureByTaskFile(1, 2, taskFileName)

    runUndoTransparentWriteAction {
      myFixture.getDocument(myFixture.file).deleteString(13, 15)
    }

    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson1")
    val task = lesson!!.getTask("task2")
    val taskFile = task!!.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(2, answerPlaceholders.size)
    assertEquals("i", answerPlaceholders[0].currentText)
    assertEquals("is another", answerPlaceholders[1].currentText)
  }

  @Test
  fun `test delete from start beyond end`() {
    val taskFileName = "taskFile1.txt"
    configureByTaskFile(1, 1, taskFileName)
    myFixture.editor.caretModel.moveToOffset(12)
    myFixture.editor.selectionModel.setSelection(12, 15)
    myFixture.type("test")

    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson1")
    val task = lesson!!.getTask("task1")
    val taskFile = task!!.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(1, answerPlaceholders.size)
    assertEquals("test", answerPlaceholders[0].currentText)
  }

  @Test
  fun `test delete before start beyond end`() {
    val taskFileName = "taskFile1.txt"
    configureByTaskFile(1, 1, taskFileName)
    myFixture.editor.caretModel.moveToOffset(12)
    myFixture.editor.selectionModel.setSelection(10, 15)
    myFixture.type("test")

    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson1")
    val task = lesson!!.getTask("task1")
    val taskFile = task!!.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(1, answerPlaceholders.size)
    assertEquals("test", answerPlaceholders[0].currentText)
  }

  @Test
  fun `test delete second placeholder start`() {
    val taskFileName = "taskFile2.txt"
    configureByTaskFile(1, 2, taskFileName)

    runUndoTransparentWriteAction {
      myFixture.getDocument(myFixture.file).deleteString(39, 41)
    }

    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson1")
    val task = lesson!!.getTask("task2")
    val taskFile = task!!.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(2, answerPlaceholders.size)
    assertEquals("is", answerPlaceholders[0].currentText)
    assertEquals("s another", answerPlaceholders[1].currentText)
  }

  @Test
  fun `test delete second placeholder end`() {
    val taskFileName = "taskFile2.txt"
    configureByTaskFile(1, 2, taskFileName)

    CommandProcessor.getInstance().runUndoTransparentAction { runWriteAction {
      myFixture.getDocument(myFixture.file).deleteString(49, 51) } }

    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson1")
    val task = lesson!!.getTask("task2")
    val taskFile = task!!.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(2, answerPlaceholders.size)
    assertEquals("is", answerPlaceholders[0].currentText)
    assertEquals("is anothe", answerPlaceholders[1].currentText)

  }

  @Test
  fun `test find and replace in whole project`() {
    val findModel = FindModel().apply {
      stringToFind = "There"
      stringToReplace = ""
      isWholeWordsOnly = true
      isFromCursor = false
      isGlobal = true
      isMultipleFiles = true
      isProjectScope = true
      isRegularExpressions = false
      isPromptOnReplace = false
    }

    val usages = mutableListOf<UsageInfo?>()
    val collector = CommonProcessors.CollectProcessor(usages)
    FindInProjectUtil
      .findUsages(findModel, project, collector, FindUsagesProcessPresentation(FindInProjectUtil.setupViewPresentation(true, findModel)))

    CommandProcessor.getInstance().executeCommand(project, {
      val replaceManager = ReplaceInProjectManager.getInstance(project)
      for (usage in usages) {
        if (usage != null) {
          replaceManager.replaceUsage(UsageInfo2UsageAdapter(usage), findModel, emptySet(), false)
        }
      }
    }, "", null)

    val placeholder1 = getCourse().findTask("lesson1", "task1").getTaskFile("taskFile1.txt")!!.answerPlaceholders[0]
    assertEquals("is", placeholder1.currentText)
    val placeholder2 = getCourse().findTask("lesson1", "task2").getTaskFile("taskFile2.txt")!!.answerPlaceholders[0]
    assertEquals("is", placeholder2.currentText)
    val placeholder3 = getCourse().findTask("lesson1", "task2").getTaskFile("taskFile2.txt")!!.answerPlaceholders[1]
    assertEquals("is another", placeholder3.currentText)
  }

  private val AnswerPlaceholder.currentText: String get() {
    val document = taskFile.getDocument(project)!!
    return document.text.substring(offset, endOffset)
  }

  @Throws(IOException::class)
  override fun createCourse() {
    myFixture.copyDirectoryToProject("lesson1", "lesson1")
    val course = EduCourse()
    course.name = "Edu test course"
    course.languageId = PlainTextLanguage.INSTANCE.id
    StudyTaskManager.getInstance(myFixture.project).course = course

    val lesson1 = createLesson(1, 2)
    course.addLesson(lesson1)
    course.init(false)
    PsiDocumentManager.getInstance(project).commitAllDocuments()
  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/documentListener"
  }
}
