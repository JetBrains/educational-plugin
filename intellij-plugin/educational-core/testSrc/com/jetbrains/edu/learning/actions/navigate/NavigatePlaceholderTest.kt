package com.jetbrains.edu.learning.actions.navigate

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.actions.NextPlaceholderAction
import com.jetbrains.edu.learning.actions.PrevPlaceholderAction
import com.jetbrains.edu.learning.courseFormat.EduCourse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.io.IOException

class NavigatePlaceholderTest : EduTestCase() {
  @Test
  fun `test next placeholder`() {
    configureByTaskFile(1, 1, "taskFile1.txt")
    val caretOffset = myFixture.caretOffset
    testAction(NextPlaceholderAction.ACTION_ID)
    assertNotEquals(caretOffset, myFixture.caretOffset)
    assertEquals(37, myFixture.caretOffset)
  }

  @Test
  fun `test previous placeholder`() {
    configureByTaskFile(1, 2, "taskFile2.txt")
    val caretOffset = myFixture.caretOffset
    testAction(PrevPlaceholderAction.ACTION_ID)
    assertNotEquals(caretOffset, myFixture.caretOffset)
    assertEquals(12, myFixture.caretOffset)
  }

  @Test
  fun `test one placeholder next`() {
    configureByTaskFile(2, 1, "taskFile1.txt")
    val caretOffset = myFixture.caretOffset
    testAction(NextPlaceholderAction.ACTION_ID)
    assertEquals(caretOffset, myFixture.caretOffset)
  }

  @Test
  fun `test one placeholder previous`() {
    configureByTaskFile(2, 1, "taskFile1.txt")
    val caretOffset = myFixture.caretOffset
    testAction(PrevPlaceholderAction.ACTION_ID)
    assertEquals(caretOffset, myFixture.caretOffset)
  }

  @Test
  fun `test last placeholder next`() {
    configureByTaskFile(1, 2, "taskFile2.txt")
    val caretOffset = myFixture.caretOffset
    testAction(NextPlaceholderAction.ACTION_ID)
    assertEquals(caretOffset, myFixture.caretOffset)
  }

  @Test
  fun `test first placeholder previous`() {
    configureByTaskFile(1, 1, "taskFile1.txt")
    val caretOffset = myFixture.caretOffset
    testAction(PrevPlaceholderAction.ACTION_ID)
    assertEquals(caretOffset, myFixture.caretOffset)
  }

  @Test
  fun `test not in placeholder next`() {
    configureByTaskFile(2, 2, "taskFile2.txt")
    val caretOffset = myFixture.caretOffset
    testAction(NextPlaceholderAction.ACTION_ID)
    assertNotEquals(caretOffset, myFixture.caretOffset)
    assertEquals(12, myFixture.caretOffset)
  }

  @Test
  fun `test not in placeholder previous`() {
    configureByTaskFile(2, 2, "taskFile2.txt")
    val caretOffset = myFixture.caretOffset
    testAction(PrevPlaceholderAction.ACTION_ID)
    assertEquals(caretOffset, myFixture.caretOffset)
  }

  @Test
  fun `test caret after placeholder next`() {
    configureByTaskFile(2, 3, "taskFile3.txt")
    val caretOffset = myFixture.caretOffset
    testAction(NextPlaceholderAction.ACTION_ID)
    assertEquals(caretOffset, myFixture.caretOffset)
  }

  @Test
  fun `test caret after placeholder previous`() {
    configureByTaskFile(2, 3, "taskFile3.txt")
    val caretOffset = myFixture.caretOffset
    testAction(PrevPlaceholderAction.ACTION_ID)
    assertNotEquals(caretOffset, myFixture.caretOffset)
    assertEquals(12, myFixture.caretOffset)
  }

  @Throws(IOException::class)
  override fun createCourse() {
    myFixture.copyDirectoryToProject("lesson1", "lesson1")
    myFixture.copyDirectoryToProject("lesson2", "lesson2")
    val course = EduCourse()
    course.name = "Edu test course"
    course.languageId = PlainTextLanguage.INSTANCE.id
    StudyTaskManager.getInstance(project).course = course
    val lesson1 = createLesson(1, 2)
    val lesson2 = createLesson(2, 3)
    course.addLesson(lesson1)
    course.addLesson(lesson2)
    course.init(false)
  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/actions/navigatePlaceholder"
  }
}
