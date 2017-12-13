package com.jetbrains.edu.learning

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.testFramework.EditorTestUtil
import com.jetbrains.edu.learning.courseFormat.Course
import java.io.IOException

class AnswerPlaceholderExtendSelectionTest : EduTestCase() {

  override fun runTest() {
    println("${AnswerPlaceholderExtendSelectionTest::class.simpleName} is temporarily disabled in AS")
  }

  fun testExtendSelection() {
    val lessonIndex = 1
    val taskIndex = 1
    val taskFileName = "taskFile1.txt"
    configureByTaskFile(lessonIndex, taskIndex, taskFileName)
    myFixture.editor.caretModel.moveToOffset(12)
    myFixture.editor.selectionModel.removeSelection()
    assertNull(myFixture.editor.selectionModel.selectedText)
    EditorTestUtil.executeAction(myFixture.editor, IdeActions.ACTION_EDITOR_SELECT_WORD_AT_CARET)
    assertEquals("is", myFixture.editor.selectionModel.selectedText)
  }

  fun testLongPlaceholder() {
    val lessonIndex = 1
    val taskIndex = 2
    val taskFileName = "taskFile2.txt"
    configureByTaskFile(lessonIndex, taskIndex, taskFileName)
    myFixture.editor.caretModel.moveToOffset(40)
    myFixture.editor.selectionModel.removeSelection()
    assertNull(myFixture.editor.selectionModel.selectedText)
    EditorTestUtil.executeAction(myFixture.editor, IdeActions.ACTION_EDITOR_SELECT_WORD_AT_CARET)
    EditorTestUtil.executeAction(myFixture.editor, IdeActions.ACTION_EDITOR_SELECT_WORD_AT_CARET)
    assertEquals("is another", myFixture.editor.selectionModel.selectedText)
  }

  @Throws(IOException::class)
  override fun createCourse() {
    myFixture.copyDirectoryToProject("lesson1", "lesson1")
    val course = Course()
    course.name = "Edu test course"
    course.language = EduNames.JAVA
    StudyTaskManager.getInstance(myFixture.project).course = course

    val lesson1 = createLesson(1, 3)
    course.addLesson(lesson1)
    course.initCourse(false)
  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/extendSelection"
  }
}
