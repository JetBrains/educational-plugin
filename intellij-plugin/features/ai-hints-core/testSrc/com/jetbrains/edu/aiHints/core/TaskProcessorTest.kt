package com.jetbrains.edu.aiHints.core

import com.intellij.testFramework.rethrowLoggedErrorsIn
import com.jetbrains.edu.learning.EduTestCase
import org.junit.Test

class TaskProcessorTest : EduTestCase() {
  /**
   * Lets our tests execute in an environment close to the production one, i.e. in a background thread and without Read Lock by default,
   * so to test that [com.jetbrains.edu.aiHints.core.TaskProcessorImpl]'s API is thread-safe.
   */
  override fun runInDispatchThread(): Boolean = false

  override fun setUp() {
    super.setUp()
    courseWithFiles {
      lesson("lesson") {
        eduTask("task") {
          taskFile("task.txt")
        }
      }
    }
    registerPlainTextEduAiHintsProcessor(testRootDisposable)
  }

  private val taskProcessor by lazy { TaskProcessor(findTask(0, 0)) }

  @Test
  fun `test getting functions from task without ReadLock`() {
    val functionsFromTask = assertNoErrorsLogged {
      taskProcessor.getFunctionsFromTask()
    }
    kotlin.test.assertNotNull(functionsFromTask)
    assertEquals(0, functionsFromTask.size)
  }

  @Test
  fun `test extracting required functions from the CodeHint without ReadLock`() {
    val requiredFunctionsFromCodeHint = assertNoErrorsLogged {
      taskProcessor.extractRequiredFunctionsFromCodeHint("")
    }
    assertEquals("", requiredFunctionsFromCodeHint)
  }

  @Test
  fun `test getting submission text representation without ReadLock`() {
    val submissionTextRepresentation = assertNoErrorsLogged {
      taskProcessor.getSubmissionTextRepresentation()
    }
    assertNull(submissionTextRepresentation)
  }

  private fun <R> assertNoErrorsLogged(runnable: () -> R): R {
    var result: R? = null
    rethrowLoggedErrorsIn {
      result = runnable()
    }
    @Suppress("UNCHECKED_CAST")
    return result as R
  }
}
