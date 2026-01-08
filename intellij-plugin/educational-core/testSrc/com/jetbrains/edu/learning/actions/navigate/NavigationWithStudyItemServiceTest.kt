package com.jetbrains.edu.learning.actions.navigate

import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.navigation.StudyItemSelectionService
import org.junit.Test

// Note, `CodeInsightTestFixture#type` can trigger completion (e.g. it inserts paired `"`)
class NavigationWithStudyItemServiceTest : NavigationTestBase() {

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

    assertEquals(
      course.findTask("lesson1", "task1").name,
      project.getCurrentTask()!!.name
    )

    // When

    StudyItemSelectionService.getInstance(project).setCurrentStudyItem(studyItemToNavigate)
    UIUtil.dispatchAllInvocationEvents()

    // Then

    assertEquals(
      "The navigated task is forced, so we must navigate to the the specified task",
      expectedTask,
      project.getCurrentTask()!!.name
    )
  }
}
