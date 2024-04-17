package com.jetbrains.edu.learning.coursera

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.courseFormat.CourseraCourse
import com.jetbrains.edu.learning.initializeCourse
import org.junit.Test


class AssignmentFormatTest : EduTestCase() {
  @Test
  fun `test assignment creation from zip`() {
    val courseraCourse = getCourseraCourse()
    assertInstanceOf(courseraCourse, CourseraCourse::class.java)
    initializeCourse(project, courseraCourse)

    val task = findTask(0, 0)

    assertTrue(task.descriptionText.isNotEmpty())

    val pathWithPlaceholders = "src/mastermind/evaluateGuess.kt"
    assertEquals(setOf(pathWithPlaceholders,
                       "src/mastermind/playMastermind.kt",
                       "test/mastermind/MastermindTestUtil.kt",
                       "test/mastermind/TestMastermindDifferentLetters.kt",
                       "test/mastermind/TestMastermindRepeatedLetters.kt",
                       CourseraTaskChecker.PART_ID,
                       CourseraTaskChecker.ASSIGNMENT_KEY), task.taskFiles.keys)

    assertFalse(findTaskFile(0, 0, CourseraTaskChecker.PART_ID).isVisible)
    assertEquals(1, findTaskFile(0, 0, pathWithPlaceholders).answerPlaceholders.size)
  }

  private fun getCourseraCourse(): CourseraCourse {
    val zipPath = "$testDataPath/${getTestName(true).trim()}.zip"
    return EduUtilsKt.getCourseraCourse(zipPath) as? CourseraCourse ?: error("Failed to import Coursera course from $zipPath")
  }

  @Test
  fun `test coursera course`() {
    val course = getCourseraCourse()
    assertInstanceOf(course, CourseraCourse::class.java)
    assertFalse(course.submitManually)
  }

  @Test
  fun `test coursera course submit manually`() {
    val course = getCourseraCourse()
    assertInstanceOf(course, CourseraCourse::class.java)
    assertTrue(course.submitManually)
  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/coursera"
  }
}