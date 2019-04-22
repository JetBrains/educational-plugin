package com.jetbrains.edu.learning.coursera

import com.intellij.testFramework.UsefulTestCase
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils


class AssignmentFormatTest : EduTestCase() {
  fun `test assignment creation from zip`() {
    val zipPath = "$testDataPath/week2.zip"
    val courseraCourse = StartCourseraAssignment.getCourseraCourse(zipPath) ?: error("Failed to import Coursera course from $zipPath")

    UsefulTestCase.assertInstanceOf(courseraCourse, CourseraCourse::class.java)
    GeneratorUtils.initializeCourse(project, courseraCourse)

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

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/coursera"
  }
}