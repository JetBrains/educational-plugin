package com.jetbrains.edu.learning.taskDescription

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import junit.framework.TestCase

class TaskDescriptionMarkdownTest : EduTestCase() {

  fun `test plain text`() {
    doTest("solve task", "<body md-src-pos=\"0..10\"><p md-src-pos=\"0..10\">solve task</p></body>")
  }

  fun `test template text`() {
    doTest("This is the markdown document.\n" +
           "\n" +
           "Write your task text here\n" +
           "\n" +
           "<div class=\"hint\">\n" +
           "  You can add hints anywhere in task text. Copy all hint div block and change its content.\n" +
           "</div>",
           "<body md-src-pos=\"0..175\"><p md-src-pos=\"0..30\">This is the markdown document.</p><p md-src-pos=\"32..57\">Write your task text here</p><div class=\"hint\">\n" +
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
           "<body md-src-pos=\"0..176\"><h1 md-src-pos=\"0..31\">This is the markdown document.</h1><p md-src-pos=\"33..58\">Write your task text here</p><div class=\"hint\">\n" +
           "  You can add hints anywhere in task text. Copy all hint div block and change its content.\n" +
           "</div>\n" +
           "</body>")
  }

  private fun doTest(descriptionText: String, expectedText: String) {
    val first = taskWithFile(descriptionText)
    val actualText = first.getTaskDescription(first.getTaskDir(project))

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