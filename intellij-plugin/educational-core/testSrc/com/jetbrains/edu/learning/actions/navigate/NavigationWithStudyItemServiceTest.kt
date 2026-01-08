package com.jetbrains.edu.learning.actions.navigate

import com.intellij.testFramework.PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue
import com.intellij.testFramework.common.DEFAULT_TEST_TIMEOUT
import com.intellij.testFramework.runInEdtAndWait
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.navigation.StudyItemSelectionService
import com.jetbrains.edu.learning.requireCurrentTask
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.Test

class NavigationWithStudyItemServiceTest : NavigationTestBase() {

  // Run NOT in EDT because the tests should wait for the process that happens both in EDT and not in EDT.
  override fun runInDispatchThread(): Boolean = false

  @Test
  fun `navigation with StudyItemSelectionService to a task in a framework lesson`() = doTest(
    studyItemToNavigate = 204,
    expectedTask = "task4"
  )

  @Test
  fun `navigation with StudyItemSelectionService to a task in a normal lesson`() = doTest(
    studyItemToNavigate = 202,
    expectedTask = "task2"
  )

  @Test
  fun `navigation with StudyItemSelectionService to a framework lesson`() = doTest(
    studyItemToNavigate = 102,
    expectedTask = "task4"
  )

  @Test
  fun `navigation with StudyItemSelectionService to a normal lesson`() = doTest(
    studyItemToNavigate = 101,
    expectedTask = "task1"
  )

  private fun doTest(studyItemToNavigate: Int, expectedTask: String) {
    // Given
    val course = courseWithFiles {
      lesson("lesson1", id = 101) {
        eduTask("task1", stepId = 201) {
          taskFile("task.txt", "text")
        }
        eduTask("task2", stepId = 202) {
          taskFile("task.txt", "text")
        }
      }
      frameworkLesson("lesson2", id = 102) {
        eduTask("task3", stepId = 203) {
          taskFile("task.txt", "text")
        }
        eduTask("task4", stepId = 204) {
          taskFile("task.txt", "text")
        }
      }
    }

    // make task4 be the current one in the framework lesson
    NavigationUtils.navigateToTask(project, course.findTask("lesson2", "task4"), forceSpecificTaskInFrameworkLesson = true)
    // return to the first task in the normal (non-framework) lesson
    NavigationUtils.navigateToTask(project, course.findTask("lesson1", "task1"))

    runInEdtAndWait {
      dispatchAllInvocationEventsInIdeEventQueue()
    }
    assertEquals(
      course.findTask("lesson1", "task1").name,
      project.requireCurrentTask().name
    )

    // We mock so that we can later wait until the method NavigationUtils.navigateToTask() is called
    mockkObject(NavigationUtils)

    // When

    StudyItemSelectionService.getInstance(project).setCurrentStudyItem(studyItemToNavigate)

    // Then

    verify(timeout = DEFAULT_TEST_TIMEOUT.inWholeMilliseconds) {
      NavigationUtils.navigateToTask(any(), any(), any(), any(), any(), any(), any())
    }

    runInEdtAndWait {
      dispatchAllInvocationEventsInIdeEventQueue()
    }
    assertEquals(
      "The navigated task is forced, so we must navigate to the the specified task",
      expectedTask,
      project.requireCurrentTask().name
    )
  }

  override fun tearDown() {
    try {
      unmockkAll()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }
}
