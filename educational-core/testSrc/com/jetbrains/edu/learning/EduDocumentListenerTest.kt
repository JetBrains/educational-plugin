package com.jetbrains.edu.learning

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
    val task = lesson.getTask("task$taskIndex")
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
    val task = lesson.getTask("task$taskIndex")
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
    val task = lesson.getTask("task$taskIndex")
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
    val task = lesson.getTask("task$taskIndex")
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
    val task = lesson.getTask("task$taskIndex")
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
    val task = lesson.getTask("task$taskIndex")
    val taskFile = task.getTaskFile(taskFileName)
    val answerPlaceholders = taskFile!!.answerPlaceholders
    assertEquals(2, answerPlaceholders.size)
    assertEquals(12, answerPlaceholders[0].offset)
    assertEquals(2, answerPlaceholders[0].length)
    assertEquals(40, answerPlaceholders[1].offset)
    assertEquals(10, answerPlaceholders[1].length)
  }

  @Throws(IOException::class)
  override fun createCourse() {
    myFixture.copyDirectoryToProject("lesson1", "lesson1")
    val course = Course()
    course.name = "Edu test course"
    course.language = EduNames.JAVA
    StudyTaskManager.getInstance(myFixture.project).course = course

    val lesson1 = createLesson(1, 2)
    course.addLesson(lesson1)
    course.initCourse(false)
  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/documentListener"
  }
}
