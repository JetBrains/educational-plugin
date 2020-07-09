package com.jetbrains.edu.learning.codeforces

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask

abstract class CodeforcesTestCase : EduTestCase() {
  override fun getTestDataPath(): String = "testData/codeforces"

  protected fun CodeforcesTask.checkTaskDescription(contestId: Int, problem: Char) {
    val expectedTaskDescription = loadText(expectedTaskDescriptionFiles.getValue(contestId).getValue(problem))
    val actualTaskDescription = descriptionText.lineSequence().joinToString(separator = "\n") { it.trimEnd() }
    assertEquals(expectedTaskDescription, actualTaskDescription)
  }

  companion object {
    const val contest1211 = "Contest 1211.html"

    val expectedTaskDescriptionFiles = mapOf(
      1170 to mapOf(
        'A' to "Contest 1170 problem A expected task description.html",
        'E' to "Contest 1170 problem E expected task description.html",
        'G' to "Contest 1170 problem G expected task description.html"
      ),
      1211 to mapOf(
        'A' to "Contest 1211 problem A expected task description.html",
        'G' to "Contest 1211 problem G expected task description.html"
      ),
      1272 to mapOf(
        'A' to "Contest 1272 problem A expected task description.html",
        'B' to "Contest 1272 problem B expected task description.html"
      )
    )
  }
}