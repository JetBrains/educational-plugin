package com.jetbrains.edu.aiHints.kotlin

import com.jetbrains.edu.aiHints.core.TaskProcessor
import com.jetbrains.edu.aiHints.core.context.AuthorSolutionContext
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.findTask
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Test

class KtStringExtractorTest : EduTestCase() {
  override fun createCourse() {
    courseWithFiles(language = KotlinLanguage.INSTANCE) {
      lesson("lesson1") {
        eduTask("task1") {
          kotlinTaskFile(
            "task.kt", """
            const val stringTemplate = "string"

            fun greet(name: String) = "Hello, \${'$'}\{name\}!"

            fun main() {
                val a = "AA"
                val b = stringTemplate
                println(a)
                println("Hello!")
            }
          """
          )
        }
      }
    }
  }

  override fun runInDispatchThread(): Boolean = false

  @Test
  fun `test strings from author solution`() {
    assertEquals(expectedStrings, AuthorSolutionContext.create(project, currentTask()).functionsToStringMap.values.flatten())
  }

  @Test
  fun `test strings from the task`() {
    assertEquals(expectedStrings, TaskProcessor(currentTask()).getStringsFromTask())
  }

  private fun currentTask(): Task = getCourse().findTask("lesson1", "task1")

  private val expectedStrings = listOf(
    """"Hello, \$\{name\}!"""",
    """"AA"""",
    """"Hello!"""",
    """"string"""",
  )
}