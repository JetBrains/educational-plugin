package com.jetbrains.edu.aiHints.core

import com.jetbrains.edu.learning.EduTestCase
import org.junit.Test

class TaskProcessorTest : EduTestCase() {
  /**
   * Let's make our tests execute without Read Lock by default
   * so to test that [com.jetbrains.edu.aiHints.core.TaskProcessorImpl]'s API is thread safe.
   */
  override fun runInDispatchThread(): Boolean = false

  override fun setUp() {
    super.setUp()
    courseWithFiles {
      lesson("lesson") {
        eduTask("task") {
          taskFile("task.txt", """
            def f():
              return 42
          """.trimIndent())
        }
      }
    }
    registerPlainTextEduAiHintsProcessor(testRootDisposable)
  }

  private val taskProcessor by lazy { TaskProcessorImpl(findTask(0, 0)) }

  @Test
  fun `test getting functions from task`() {
    val functionsFromTask = taskProcessor.getFunctionsFromTask()
    assertNotNull(functionsFromTask)
    assertEquals(0, functionsFromTask?.size)
  }

  @Test
  fun `test extracting required functions from the CodeHint`() {
    // TaskProcessorImpl#extractRequiredFunctionsFromCodeHint
  }

  @Test
  fun `test getting submission text representation`() {
    // TaskProcessorImpl#getSubmissionTextRepresentation
  }
}