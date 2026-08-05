package com.jetbrains.edu.learning.projectView

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.findLesson
import com.jetbrains.edu.learning.findSection
import com.jetbrains.edu.learning.findTask
import org.junit.Test

/**
 * Checks which study items [CourseViewVisibleItems] reports as the ones to be shown to a learner
 * depending on the subtree mode and the items already marked as visible.
 */
class CourseViewVisibleItemsTest : EduTestCase() {

  @Test
  fun `test everything is visible while subtree mode is disabled`() {
    // given
    val course = createCourseWithIds()
    assertFalse(CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled)

    // when
    val task1 = course.findTask("lesson1", "task1", "section1")
    CourseViewVisibleItems.getInstance(project).markStudyItemAsVisible(task1)

    // then
    // nothing is revealed while the subtree mode is disabled because everything is shown anyway
    assertEmpty(CourseViewVisibleItems.getInstance(project).visibleTaskIds)
    assertVisible(course.allItems)
  }

  @Test
  fun `test nothing except the course itself is visible until something is revealed`() {
    // given
    val course = createCourseWithIds()

    // when
    CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled = true

    // then
    assertTrue(CourseViewVisibleItems.getInstance(project).shouldBeShown(course))
    assertHidden(course.allItems - course)
  }

  @Test
  fun `test revealed task makes its lesson and section visible`() {
    // given
    val course = createCourseWithIds()
    CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled = true

    // when
    val task3 = course.findTask("lesson2", "task3", "section1")
    CourseViewVisibleItems.getInstance(project).markStudyItemAsVisible(task3)

    // then
    val section1 = course.findSection("section1")
    val section2 = course.findSection("section2")
    val lesson1 = course.findLesson("lesson1", "section1")
    val lesson2 = course.findLesson("lesson2", "section1")
    val task4 = course.findTask("lesson2", "task4", "section1")
    assertVisible(section1, lesson2, task3)
    // sibling task, sibling lesson and the other section are still hidden
    assertHidden(task4, lesson1, section2)
  }

  @Test
  fun `test revealed lesson makes all its tasks visible`() {
    // given
    val course = createCourseWithIds()
    CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled = true

    // when
    val lesson1 = course.findLesson("lesson1", "section1")
    CourseViewVisibleItems.getInstance(project).markStudyItemAsVisible(lesson1)

    // then
    val section1 = course.findSection("section1")
    val lesson2 = course.findLesson("lesson2", "section1")
    assertVisible(lesson1.allItems + section1)
    assertHidden(lesson2.allItems)
  }

  @Test
  fun `test revealed section makes all its lessons visible`() {
    // given
    val course = createCourseWithIds()
    CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled = true

    // when
    val section1 = course.findSection("section1")
    CourseViewVisibleItems.getInstance(project).markStudyItemAsVisible(section1)

    // then
    val section2 = course.findSection("section2")
    assertVisible(section1.allItems)
    assertHidden(section2.allItems)
  }

  @Test
  fun `test revealed items are forgotten when subtree mode is disabled`() {
    // given
    val course = createCourseWithIds()
    CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled = true
    val task1 = course.findTask("lesson1", "task1", "section1")
    CourseViewVisibleItems.getInstance(project).markStudyItemAsVisible(task1)
    assertEquals(setOf(111), CourseViewVisibleItems.getInstance(project).visibleTaskIds)

    // when
    CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled = false

    // then
    assertEmpty(CourseViewVisibleItems.getInstance(project).visibleTaskIds)
    assertVisible(course.allItems)
  }

  @Test
  fun `test nothing is visible when subtree mode is enabled again`() {
    // given
    val course = createCourseWithIds()
    CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled = true
    CourseViewVisibleItems.getInstance(project).markStudyItemAsVisible(course.findTask("lesson1", "task1", "section1"))
    CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled = false

    // when
    CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled = true

    // then
    // previously revealed items are not restored
    assertEmpty(CourseViewVisibleItems.getInstance(project).visibleTaskIds)
    assertTrue(CourseViewVisibleItems.getInstance(project).shouldBeShown(course))
    assertHidden(course.allItems - course)
  }

  @Test
  fun `test all tasks of framework lesson are visible together with the lesson`() {
    // given
    val course = courseWithFiles {
      frameworkLesson("framework lesson", id = 1) {
        eduTask("task1", stepId = 11) { taskFile("Task.txt") }
        eduTask("task2", stepId = 12) { taskFile("Task.txt") }
      }
      lesson("lesson", id = 2) {
        eduTask("task3", stepId = 21) { taskFile("Task.txt") }
      }
    }
    CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled = true

    // when
    val task2 = course.findTask("framework lesson", "task2")
    CourseViewVisibleItems.getInstance(project).markStudyItemAsVisible(task2)

    // then
    val frameworkLesson = course.findLesson("framework lesson")
    val lesson = course.findLesson("lesson")
    // a framework lesson is always shown as a single item, so all its tasks are visible at once
    assertVisible(frameworkLesson.allItems)
    assertHidden(lesson.allItems)
  }

  @Test
  fun `test everything is visible for course without study item ids`() {
    // given
    val course = courseWithFiles {
      lesson("lesson") {
        eduTask("task1") { taskFile("Task.txt") }
        eduTask("task2") { taskFile("Task.txt") }
      }
    }
    CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled = true

    // when
    val lesson = course.findLesson("lesson")
    CourseViewVisibleItems.getInstance(project).markStudyItemAsVisible(lesson)

    // then
    assertEmpty(CourseViewVisibleItems.getInstance(project).visibleTaskIds)
    assertVisible(course.allItems)
  }

  @Test
  fun `test everything is visible in course creator mode`() {
    // given
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson", id = 1) {
        eduTask("task1", stepId = 11) { taskFile("Task.txt") }
        eduTask("task2", stepId = 12) { taskFile("Task.txt") }
      }
    }
    CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled = true

    // when
    val task1 = course.findTask("lesson", "task1")
    CourseViewVisibleItems.getInstance(project).markStudyItemAsVisible(task1)

    // then
    assertVisible(course.allItems)
  }

  private fun createCourseWithIds(): Course = courseWithFiles {
    section("section1", id = 1) {
      lesson("lesson1", id = 11) {
        eduTask("task1", stepId = 111) { taskFile("Task.txt") }
        eduTask("task2", stepId = 112) { taskFile("Task.txt") }
      }
      lesson("lesson2", id = 12) {
        eduTask("task3", stepId = 121) { taskFile("Task.txt") }
        eduTask("task4", stepId = 122) { taskFile("Task.txt") }
      }
    }
    section("section2", id = 2) {
      lesson("lesson3", id = 21) {
        eduTask("task5", stepId = 211) { taskFile("Task.txt") }
      }
    }
  }

  private fun assertVisible(vararg items: StudyItem) {
    assertVisible(items.toList())
  }

  private fun assertVisible(items: List<StudyItem>) {
    val hidden = items.filter { !CourseViewVisibleItems.getInstance(project).shouldBeShown(it) }
    assertEmpty("Items expected to be visible: ${hidden.joinToString { it.pathInCourse }}", hidden)
  }

  private fun assertHidden(vararg items: StudyItem) {
    assertHidden(items.toList())
  }

  private fun assertHidden(items: List<StudyItem>) {
    val visible = items.filter { CourseViewVisibleItems.getInstance(project).shouldBeShown(it) }
    assertEmpty("Items expected to be hidden: ${visible.joinToString { it.pathInCourse }}", visible)
  }

  private val StudyItem.allItems: List<StudyItem>
    get() = when (this) {
      is Course -> listOf(this) + sections.flatMap { it.allItems } + lessons.flatMap { it.allItems }
      is Section -> listOf(this) + lessons.flatMap { it.allItems }
      is Lesson -> listOf(this) + taskList
      is Task -> listOf(this)
      else -> emptyList()
    }
}
