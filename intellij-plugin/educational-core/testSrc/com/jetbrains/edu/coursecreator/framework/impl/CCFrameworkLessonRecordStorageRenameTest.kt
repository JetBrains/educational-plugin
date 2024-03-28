package com.jetbrains.edu.coursecreator.framework.impl

import com.jetbrains.edu.coursecreator.framework.CCFrameworkLessonManager
import com.jetbrains.edu.learning.actions.rename.RenameTestBase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.*

class CCFrameworkLessonRecordStorageRenameTest : RenameTestBase() {
  private val manager: CCFrameworkLessonManager
    get() = CCFrameworkLessonManager.getInstance(project)

  fun `test record is not deleted after task rename action`() {
    val course = createFrameworkCourse()
    val lesson = getFrameworkLesson(course)

    val task1 = lesson.getTask("task1")!!
    val record1 = manager.getRecord(task1)

    doRenameAction(course, "section1/lesson1/task1", "task3")

    with(manager) {
      assertEquals(2, state.taskRecords.size)
      assertNull(getRecord("section1/lesson1/task1"))
      assertEquals(record1, getRecord("section1/lesson1/task3"))
    }
  }

  fun `test record is not deleted after lesson rename action`() {
    val course = createFrameworkCourse()
    val lesson = getFrameworkLesson(course)

    val task1 = lesson.getTask("task1")!!
    val task2 = lesson.getTask("task2")!!
    val record1 = manager.getRecord(task1)
    val record2 = manager.getRecord(task2)

    doRenameAction(course, "section1/lesson1", "lesson2")

    with(manager) {
      assertEquals(2, state.taskRecords.size)
      assertNull(getRecord("section1/lesson1/task1"))
      assertEquals(record1, getRecord("section1/lesson2/task1"))
      assertNull(getRecord("section1/lesson1/task2"))
      assertEquals(record2, getRecord("section1/lesson2/task2"))
    }
  }

  fun `test record is not deleted after section rename action`() {
    val course = createFrameworkCourse()
    val lesson = getFrameworkLesson(course)

    val task1 = lesson.getTask("task1")!!
    val task2 = lesson.getTask("task2")!!
    val record1 = manager.getRecord(task1)
    val record2 = manager.getRecord(task2)

    doRenameAction(course, "section1", "section2")

    with(manager) {
      assertEquals(2, state.taskRecords.size)
      assertNull(getRecord("section1/lesson1/task1"))
      assertEquals(record1, getRecord("section2/lesson1/task1"))
      assertNull(getRecord("section1/lesson1/task2"))
      assertEquals(record2, getRecord("section2/lesson1/task2"))
    }
  }

  private fun createFrameworkCourse(): Course = courseWithFiles(
    courseMode = CourseMode.EDUCATOR,
    language = FakeGradleBasedLanguage,
  ) {
    section("section1") {
      frameworkLesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo()")
        }
        eduTask("task2") {
          taskFile("Task.kt", "fun foo()")
        }
      }
    }
  }.apply {
    val lesson = getFrameworkLesson(this)
    for ((index, task) in lesson.taskList.withIndex()) {
      manager.updateRecord(task, index)
    }
  }

  private fun getFrameworkLesson(course: Course): FrameworkLesson {
    return course.getSection("section1")?.getLesson("lesson1") as FrameworkLesson
  }
}