package com.jetbrains.edu.learning.stepik

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import org.junit.Test

class StepikStepsTest : EduTestCase() {

  @Test
  fun `test md description converted to html`() {
    val course = courseWithFiles {
      lesson {
        eduTask(taskDescription = "## This is the code:\n ```\n version \n ```\n This was the code.",
                taskDescriptionFormat = DescriptionFormat.MD)
      }
    }
    val task = course.findTask("lesson1", "task1")
    val step = Step(project, task)

    assertEquals("<body><h2>This is the code:</h2><pre><code>version \n" +
                 "</code></pre><p>This was the code.</p></body>".trimIndent(), step.text.trimIndent())
  }

  @Test
  fun `test no conversion for hyperskill course`() {
    val course = courseWithFiles(courseProducer = ::HyperskillCourse) {
      lesson {
        eduTask(taskDescription = "## This is the code:\n ```\n version \n ```\n This was the code.",
                taskDescriptionFormat = DescriptionFormat.MD)
      }
    }
    val task = course.findTask("lesson1", "task1")
    val step = Step(project, task)

    assertEquals("## This is the code:\n ```\n version \n ```\n This was the code.".trimIndent(),
                 step.text.trimIndent())

  }
}
