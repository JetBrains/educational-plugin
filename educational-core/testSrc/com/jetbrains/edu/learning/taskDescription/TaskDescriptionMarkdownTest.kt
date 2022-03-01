package com.jetbrains.edu.learning.taskDescription

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.intellij.lang.annotations.Language

class TaskDescriptionMarkdownTest : EduTestCase() {

  fun `test plain text`() = doTest("solve task", "<body><p>solve task</p></body>")

  fun `test template text`() = doTest("""
    This is the markdown document.

    Write your task text here
    
    <div class="hint">
      You can add hints anywhere in task text. Copy all hint div block and change its content.
    </div>
  """, """
    <body><p>This is the markdown document.</p><p>Write your task text here</p><div class="hint">
      You can add hints anywhere in task text. Copy all hint div block and change its content.
    </div>
    </body>
  """)

  fun `test text with header`() = doTest("""
    # This is the markdown document.

    Write your task text here
    
    <div class="hint">
      You can add hints anywhere in task text. Copy all hint div block and change its content.
    </div>
  """, """
    <body><h1>This is the markdown document.</h1><p>Write your task text here</p><div class="hint">
      You can add hints anywhere in task text. Copy all hint div block and change its content.
    </div>
    </body>
  """)

  private fun doTest(@Language("Markdown") descriptionText: String, @Language("HTML") expectedText: String) {
    val first = taskWithFile(descriptionText.trimIndent())
    val actualText = EduUtils.getTaskTextFromTask(project, first)

    assertEquals("Task description text mismatch", expectedText.trimIndent(), actualText)
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
