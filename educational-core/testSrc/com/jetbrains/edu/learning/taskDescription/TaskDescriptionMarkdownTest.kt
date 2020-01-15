package com.jetbrains.edu.learning.taskDescription

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import junit.framework.TestCase

class TaskDescriptionMarkdownTest : EduTestCase() {

  fun `test plain text`() {
    doTest("solve task", "<body><p>solve task</p></body>")
  }

  fun `test template text`() {
    doTest("This is the markdown document.\n" +
           "\n" +
           "Write your task text here\n" +
           "\n" +
           "<div class=\"hint\">\n" +
           "  You can add hints anywhere in task text. Copy all hint div block and change its content.\n" +
           "</div>",
           "<body><p>This is the markdown document.</p><p>Write your task text here</p><div class=\"hint\">\n" +
           "  You can add hints anywhere in task text. Copy all hint div block and change its content.\n" +
           "</div>\n" +
           "</body>")
  }

  fun `test text with header`() {
    doTest("#This is the markdown document.\n" +
           "\n" +
           "Write your task text here\n" +
           "\n" +
           "<div class=\"hint\">\n" +
           "  You can add hints anywhere in task text. Copy all hint div block and change its content.\n" +
           "</div>",
           "<body><h1>This is the markdown document.</h1><p>Write your task text here</p><div class=\"hint\">\n" +
           "  You can add hints anywhere in task text. Copy all hint div block and change its content.\n" +
           "</div>\n" +
           "</body>")
  }

  private fun doTest(descriptionText: String, expectedText: String) {
    val first = taskWithFile(descriptionText)
    val actualText = EduUtils.getTaskTextFromTask(project, first)

    TestCase.assertEquals("Task description text mismatch. Expected: ${expectedText} Actual: ${actualText}",
                        expectedText, actualText)
  }

  private fun taskWithFile(taskDescription: String): Task {
    val courseWithFiles = courseWithFiles {
      lesson {
        eduTask(taskDescriptionFormat = DescriptionFormat.MD, taskDescription = taskDescription)
      }
    }

    return courseWithFiles.lessons.first().taskList.first()
  }

}