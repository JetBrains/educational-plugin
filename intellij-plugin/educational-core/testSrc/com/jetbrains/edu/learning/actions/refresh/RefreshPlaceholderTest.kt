package com.jetbrains.edu.learning.actions.refresh

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.actions.RefreshAnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.testAction
import org.junit.Test
import java.io.IOException

class RefreshPlaceholderTest : EduTestCase() {
  @Test
  fun `test refresh placeholder`() {
    configureByTaskFile(1, 1, "taskFile1.txt")
    myFixture.editor.caretModel.moveToOffset(12)
    myFixture.type("test")
    testAction(RefreshAnswerPlaceholder.ACTION_ID)
    assertEquals("Look! There is placeholder.", myFixture.getDocument(myFixture.file).text)
  }

  @Test
  fun `test caret outside`() {
    configureByTaskFile(1, 2, "taskFile2.txt")
    myFixture.editor.caretModel.moveToOffset(2)
    myFixture.type("test")
    myFixture.editor.caretModel.moveToOffset(2)
    testAction(RefreshAnswerPlaceholder.ACTION_ID, shouldBeEnabled = false)
  }

  @Test
  fun `test second refresh placeholder`() {
    findTask(0, 2).openTaskFileInEditor("taskFile3.txt", 0)
    myFixture.editor.caretModel.moveToOffset(16)
    myFixture.type("test")
    myFixture.editor.caretModel.moveToOffset(52)
    myFixture.type("test")
    testAction(RefreshAnswerPlaceholder.ACTION_ID)
    assertEquals("""
      Look! There is test placeholder.
      Look! There is second placeholder.
      """.trimIndent(), myFixture.getDocument(myFixture.file).text)
  }

  @Test
  fun `test refresh second placeholder start offset`() {
    findTask(0, 2).openTaskFileInEditor("taskFile3.txt", 0)
    myFixture.editor.caretModel.moveToOffset(16)
    myFixture.type("test test")
    myFixture.editor.caretModel.moveToOffset(56)
    myFixture.type("test")
    testAction(RefreshAnswerPlaceholder.ACTION_ID)
    assertEquals("""
      Look! There is test test placeholder.
      Look! There is second placeholder.
      """.trimIndent(), myFixture.getDocument(myFixture.file).text)
    val course = StudyTaskManager.getInstance(project).course
    val lesson = course!!.getLesson("lesson1")
    val task = lesson!!.getTask("task3")
    val taskFile = task!!.getTaskFile("taskFile3.txt")
    val placeholders = taskFile!!.answerPlaceholders
    assertEquals(2, placeholders.size)
    val secondPlaceholder = placeholders[1]
    assertEquals(53, secondPlaceholder.offset)
  }

  @Test
  fun `test first refresh placeholder`() {
    findTask(0, 2).openTaskFileInEditor("taskFile3.txt", 0)
    myFixture.editor.caretModel.moveToOffset(16)
    myFixture.type("test")
    myFixture.editor.caretModel.moveToOffset(52)
    myFixture.type("test")
    myFixture.editor.caretModel.moveToOffset(16)
    testAction(RefreshAnswerPlaceholder.ACTION_ID)
    assertEquals("""
      Look! There is first placeholder.
      Look! There is secotestnd placeholder.
      """.trimIndent(), myFixture.getDocument(myFixture.file).text)
  }

  @Throws(IOException::class)
  override fun createCourse() {
    myFixture.copyDirectoryToProject("lesson1", "lesson1")
    val course = EduCourse()
    course.name = "Edu test course"
    course.languageId = PlainTextLanguage.INSTANCE.id
    StudyTaskManager.getInstance(myFixture.project).course = course
    val lesson1 = createLesson(1, 3)
    course.addLesson(lesson1)
    course.init(false)
  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/actions/refreshPlaceholder"
  }
}
