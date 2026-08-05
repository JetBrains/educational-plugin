package com.jetbrains.edu.learning.projectView

import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.testAction
import org.junit.Test

/**
 * Checks that a task is marked as visible in [CourseViewVisibleItems] when its file is opened in an editor,
 * no matter how the file was opened.
 */
class CourseViewVisibleItemsRevealTest : EduActionTestCase() {

  @Test
  fun `test task is marked as visible when its file is opened in editor`() {
    // given
    val course = createCourseWithIds()
    CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled = true

    // when
    course.findTask("lesson2", "task1").openTaskFileInEditor("Task.txt")

    // then
    assertEquals(setOf(21), CourseViewVisibleItems.getInstance(project).visibleTaskIds)
  }

  @Test
  fun `test task is marked as visible on navigation to the next task`() {
    // given
    val course = createCourseWithIds()
    CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled = true
    course.findTask("lesson1", "task1").openTaskFileInEditor("Task.txt")

    // when
    testAction(NextTaskAction.ACTION_ID)

    // then
    assertEquals(setOf(11, 12), CourseViewVisibleItems.getInstance(project).visibleTaskIds)
  }

  @Test
  fun `test nothing is marked as visible when non-task file is opened in editor`() {
    // given
    createCourseWithIds()
    CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled = true

    // when
    myFixture.openFileInEditor(findFile("README.md"))

    // then
    assertEmpty(CourseViewVisibleItems.getInstance(project).visibleTaskIds)
  }

  @Test
  fun `test nothing is marked as visible while subtree mode is disabled`() {
    // given
    val course = createCourseWithIds()
    assertFalse(CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled)

    // when
    course.findTask("lesson2", "task1").openTaskFileInEditor("Task.txt")

    // then
    assertEmpty(CourseViewVisibleItems.getInstance(project).visibleTaskIds)
  }

  private fun createCourseWithIds(): Course = courseWithFiles {
    lesson("lesson1", id = 1) {
      eduTask("task1", stepId = 11) { taskFile("Task.txt") }
      eduTask("task2", stepId = 12) { taskFile("Task.txt") }
    }
    lesson("lesson2", id = 2) {
      eduTask("task1", stepId = 21) { taskFile("Task.txt") }
    }
    additionalFile("README.md", "Course description")
  }
}
