package com.jetbrains.edu.learning

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.jetbrains.edu.learning.courseFormat.Course
import java.io.IOException

class EduDocumentListenerTest : EduTestCase() {

  fun testTypeInPlaceholder() {
    val lessonIndex = 1
    val taskIndex = 1
    val taskFileName = "taskFile1.txt"
    configureByTaskFile(lessonIndex, taskIndex, taskFileName)
    myFixture.editor.caretModel.moveToOffset(13)
    myFixture.editor.selectionModel.removeSelection()
    myFixture.type("test")
    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson$lessonIndex")
    val task = lesson!!.getTask("task$taskIndex")
    val taskFile = task.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(1, answerPlaceholders.size)
    assertEquals(12, answerPlaceholders[0].offset)
    assertEquals(6, answerPlaceholders[0].length)
  }

  fun testTypeBeforePlaceholder() {
    val lessonIndex = 1
    val taskIndex = 1
    val taskFileName = "taskFile1.txt"
    configureByTaskFile(lessonIndex, taskIndex, taskFileName)
    myFixture.editor.caretModel.moveToOffset(1)
    myFixture.editor.selectionModel.removeSelection()
    myFixture.type("test")
    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson$lessonIndex")
    val task = lesson!!.getTask("task$taskIndex")
    val taskFile = task.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(1, answerPlaceholders.size)
    assertEquals(16, answerPlaceholders[0].offset)
    assertEquals(2, answerPlaceholders[0].length)
  }

  fun testTypeBetweenPlaceholder() {
    val lessonIndex = 1
    val taskIndex = 2
    val taskFileName = "taskFile2.txt"
    configureByTaskFile(lessonIndex, taskIndex, taskFileName)
    myFixture.editor.caretModel.moveToOffset(16)
    myFixture.editor.selectionModel.removeSelection()
    myFixture.type("test")
    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson$lessonIndex")
    val task = lesson!!.getTask("task$taskIndex")
    val taskFile = task.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(2, answerPlaceholders.size)
    assertEquals(12, answerPlaceholders[0].offset)
    assertEquals(2, answerPlaceholders[0].length)
    assertEquals(44, answerPlaceholders[1].offset)
    assertEquals(10, answerPlaceholders[1].length)
  }

  fun testTypeInSecondPlaceholder() {
    val lessonIndex = 1
    val taskIndex = 2
    val taskFileName = "taskFile2.txt"
    configureByTaskFile(lessonIndex, taskIndex, taskFileName)
    myFixture.editor.caretModel.moveToOffset(40)
    myFixture.editor.selectionModel.removeSelection()
    myFixture.type("test")
    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson$lessonIndex")
    val task = lesson!!.getTask("task$taskIndex")
    val taskFile = task.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(2, answerPlaceholders.size)
    assertEquals(12, answerPlaceholders[0].offset)
    assertEquals(2, answerPlaceholders[0].length)
    assertEquals(40, answerPlaceholders[1].offset)
    assertEquals(14, answerPlaceholders[1].length)
  }

  fun testTypeInFirstPlaceholder() {
    val lessonIndex = 1
    val taskIndex = 2
    val taskFileName = "taskFile2.txt"
    configureByTaskFile(lessonIndex, taskIndex, taskFileName)
    myFixture.editor.caretModel.moveToOffset(12)
    myFixture.editor.selectionModel.removeSelection()
    myFixture.type("test")
    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson$lessonIndex")
    val task = lesson!!.getTask("task$taskIndex")
    val taskFile = task.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(2, answerPlaceholders.size)
    assertEquals(12, answerPlaceholders[0].offset)
    assertEquals(6, answerPlaceholders[0].length)
    assertEquals(44, answerPlaceholders[1].offset)
    assertEquals(10, answerPlaceholders[1].length)
  }

  fun testTypeAfterLastPlaceholder() {
    val lessonIndex = 1
    val taskIndex = 2
    val taskFileName = "taskFile2.txt"
    configureByTaskFile(lessonIndex, taskIndex, taskFileName)
    myFixture.editor.caretModel.moveToOffset(55)
    myFixture.editor.selectionModel.removeSelection()
    myFixture.type("test")
    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson$lessonIndex")
    val task = lesson!!.getTask("task$taskIndex")
    val taskFile = task.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(2, answerPlaceholders.size)
    assertEquals(12, answerPlaceholders[0].offset)
    assertEquals(2, answerPlaceholders[0].length)
    assertEquals(40, answerPlaceholders[1].offset)
    assertEquals(10, answerPlaceholders[1].length)
  }

  fun testNewLineAtTheBeginningPlaceholder() {
    val lessonIndex = 1
    val taskIndex = 1
    val taskFileName = "taskFile1.txt"
    configureByTaskFile(lessonIndex, taskIndex, taskFileName)
    myFixture.editor.caretModel.moveToOffset(13)
    myFixture.editor.selectionModel.removeSelection()
    myFixture.type("\n")
    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson$lessonIndex")
    val task = lesson!!.getTask("task$taskIndex")
    val taskFile = task.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(1, answerPlaceholders.size)
    assertEquals(12, answerPlaceholders[0].offset)
    assertEquals(3, answerPlaceholders[0].length)
  }

  fun testDeleteBeforePlaceholder() {
    val taskFileName = "taskFile2.txt"
    configureByTaskFile(1, 2, taskFileName)

    CommandProcessor.getInstance().runUndoTransparentAction({ runWriteAction {
      myFixture.getDocument(myFixture.file).deleteString(5, 11) } })

    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson1")
    val task = lesson!!.getTask("task2")
    val taskFile = task.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(2, answerPlaceholders.size)
    assertEquals(6, answerPlaceholders[0].offset)
    assertEquals(2, answerPlaceholders[0].length)
    assertEquals(34, answerPlaceholders[1].offset)
    assertEquals(10, answerPlaceholders[1].length)
  }

  fun testDeleteBetweenPlaceholder() {
    val taskFileName = "taskFile2.txt"
    configureByTaskFile(1, 2, taskFileName)

    CommandProcessor.getInstance().runUndoTransparentAction({ runWriteAction {
      myFixture.getDocument(myFixture.file).deleteString(33, 39) } })

    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson1")
    val task = lesson!!.getTask("task2")
    val taskFile = task.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(2, answerPlaceholders.size)
    assertEquals(12, answerPlaceholders[0].offset)
    assertEquals(2, answerPlaceholders[0].length)
    assertEquals(34, answerPlaceholders[1].offset)
    assertEquals(10, answerPlaceholders[1].length)
  }

  fun testDeleteAfterPlaceholder() {
    val taskFileName = "taskFile2.txt"
    configureByTaskFile(1, 2, taskFileName)

    CommandProcessor.getInstance().runUndoTransparentAction({ runWriteAction {
      myFixture.getDocument(myFixture.file).deleteString(55, 58) } })

    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson1")
    val task = lesson!!.getTask("task2")
    val taskFile = task.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(2, answerPlaceholders.size)
    assertEquals(12, answerPlaceholders[0].offset)
    assertEquals(2, answerPlaceholders[0].length)
    assertEquals(40, answerPlaceholders[1].offset)
    assertEquals(10, answerPlaceholders[1].length)
  }

  fun testDeleteFirstPlaceholderStart() {
    val taskFileName = "taskFile2.txt"
    configureByTaskFile(1, 2, taskFileName)

    CommandProcessor.getInstance().runUndoTransparentAction({ runWriteAction {
      myFixture.getDocument(myFixture.file).deleteString(11, 13) } })

    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson1")
    val task = lesson!!.getTask("task2")
    val taskFile = task.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(2, answerPlaceholders.size)
    assertEquals(11, answerPlaceholders[0].offset)
    assertEquals(1, answerPlaceholders[0].length)
    assertEquals(38, answerPlaceholders[1].offset)
    assertEquals(10, answerPlaceholders[1].length)
  }

  fun testDeleteFirstPlaceholderEnd() {
    val taskFileName = "taskFile2.txt"
    configureByTaskFile(1, 2, taskFileName)

    CommandProcessor.getInstance().runUndoTransparentAction({ runWriteAction {
      myFixture.getDocument(myFixture.file).deleteString(13, 15) } })

    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson1")
    val task = lesson!!.getTask("task2")
    val taskFile = task.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(2, answerPlaceholders.size)
    assertEquals(12, answerPlaceholders[0].offset)
    assertEquals(1, answerPlaceholders[0].length)
    assertEquals(38, answerPlaceholders[1].offset)
    assertEquals(10, answerPlaceholders[1].length)
  }

  fun testDeleteSecondPlaceholderStart() {
    val taskFileName = "taskFile2.txt"
    configureByTaskFile(1, 2, taskFileName)

    CommandProcessor.getInstance().runUndoTransparentAction({ runWriteAction {
      myFixture.getDocument(myFixture.file).deleteString(39, 41) } })

    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson1")
    val task = lesson!!.getTask("task2")
    val taskFile = task.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(2, answerPlaceholders.size)
    assertEquals(12, answerPlaceholders[0].offset)
    assertEquals(2, answerPlaceholders[0].length)
    assertEquals(39, answerPlaceholders[1].offset)
    assertEquals(9, answerPlaceholders[1].length)
  }

  fun testDeleteSecondPlaceholderEnd() {
    val taskFileName = "taskFile2.txt"
    configureByTaskFile(1, 2, taskFileName)

    CommandProcessor.getInstance().runUndoTransparentAction({ runWriteAction {
      myFixture.getDocument(myFixture.file).deleteString(49, 51) } })

    val lesson = StudyTaskManager.getInstance(myFixture.project).course!!.getLesson("lesson1")
    val task = lesson!!.getTask("task2")
    val taskFile = task.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(2, answerPlaceholders.size)
    assertEquals(12, answerPlaceholders[0].offset)
    assertEquals(2, answerPlaceholders[0].length)
    assertEquals(40, answerPlaceholders[1].offset)
    assertEquals(9, answerPlaceholders[1].length)
  }

  @Throws(IOException::class)
  override fun createCourse() {
    myFixture.copyDirectoryToProject("lesson1", "lesson1")
    val course = Course()
    course.name = "Edu test course"
    course.language = PlainTextLanguage.INSTANCE.id
    StudyTaskManager.getInstance(myFixture.project).course = course

    val lesson1 = createLesson(1, 2)
    course.addLesson(lesson1)
    course.init(null, null, false)
  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/documentListener"
  }
}
