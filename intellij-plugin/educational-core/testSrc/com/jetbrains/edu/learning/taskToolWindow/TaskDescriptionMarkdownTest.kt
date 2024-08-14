package com.jetbrains.edu.learning.taskToolWindow

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.ext.getTaskText
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.intellij.lang.annotations.Language
import org.junit.Test

class TaskDescriptionMarkdownTest : EduTestCase() {

  @Test
  fun `test plain text`() = doTest("solve task", "<body><p>solve task</p></body>")

  @Test
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

  @Test
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

  @Test
  fun `test text with links`() = doTest("""
    [file_link](file://lesson1/task1/Task.txt)
    [course_file_link](course://lesson1/task1/Task.txt)
  """, """
    <body><p><a href="file://lesson1/task1/Task.txt">file_link</a>
    <a href="course://lesson1/task1/Task.txt">course_file_link</a></p></body>
  """)

  private fun doTest(@Language("Markdown") descriptionText: String, @Language("HTML") expectedText: String) {
    val first = taskWithFile(descriptionText.trimIndent())
    val actualText = first.getTaskText(project)

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
