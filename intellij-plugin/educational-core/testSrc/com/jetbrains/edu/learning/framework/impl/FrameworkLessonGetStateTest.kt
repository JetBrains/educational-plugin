package com.jetbrains.edu.learning.framework.impl

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.testAction
import org.junit.Test

class FrameworkLessonGetTaskStateTest : EduTestCase() {

  @Test
  fun `test getTaskState returns correct text for non-current task`() {
    val course = createFrameworkCourse()
    val lesson = course.lessons.first() as FrameworkLesson

    val task1 = lesson.taskList[0]

    withVirtualFileListener(course) {
      task1.openTaskFileInEditor("src/Task.kt")
      myFixture.type("0")
      task1.openTaskFileInEditor("src/Baz.kt")
      myFixture.type("1")
      testAction(NextTaskAction.ACTION_ID)
    }

    val actualState = FrameworkLessonManager.getInstance(project).getTaskState(lesson, task1)

    val expectedState = mapOf(
      "src/Task.kt" to "0fun foo() {}",
      "src/Baz.kt" to "1fun baz() {}",
      "test/Tests1.kt" to "fun test1() {}"
    )

    assertEquals(expectedState, actualState)
  }

  @Test
  fun `test getTaskState returns correct text in current task`() {
    val course = createFrameworkCourse()
    val lesson = course.lessons.first() as FrameworkLesson

    val task1 = lesson.taskList[0]
    val task2 = lesson.taskList[1]

    withVirtualFileListener(course) {
      task1.openTaskFileInEditor("src/Task.kt")
      testAction(NextTaskAction.ACTION_ID)
      task2.openTaskFileInEditor("src/Task.kt")
      myFixture.type("0")
      task2.openTaskFileInEditor("src/Baz.kt")
      myFixture.type("1")
    }

    val actualState = FrameworkLessonManager.getInstance(project).getTaskState(lesson, task2)

    val expectedState = mapOf(
      "src/Task.kt" to "0fun foo() {}",
      "src/Baz.kt" to "1fun baz() {}",
      "test/Tests2.kt" to "fun test2() {}"
    )

    assertEquals(expectedState, actualState)
  }

  @Test
  fun `test getTaskState throws exception when given task is not a part of given lesson`() {
    val course = createFrameworkCourse()
    val lesson = course.lessons.first() as FrameworkLesson
    val otherLesson = course.lessons[1] as FrameworkLesson

    val task1 = lesson.taskList[0]
    val otherTask = otherLesson.taskList.first()

    task1.openTaskFileInEditor("src/Task.kt")
    testAction(NextTaskAction.ACTION_ID)

    assertThrows(IllegalArgumentException::class.java) {
      FrameworkLessonManager.getInstance(project).getTaskState(lesson, otherTask)
    }
  }

  private fun createFrameworkCourse(): Course = courseWithFiles(
    language = FakeGradleBasedLanguage,
    courseProducer = ::EduCourse,
  ) {
    frameworkLesson("lesson") {
      eduTask("task1") {
        taskFile("src/Task.kt", "fun foo() {}")
        taskFile("src/Baz.kt", "fun baz() {}")
        taskFile("test/Tests1.kt", "fun test1() {}")
      }
      eduTask("task2") {
        taskFile("src/Task.kt", "fun foo() {}")
        taskFile("src/Baz.kt", "fun baz() {}")
        taskFile("test/Tests2.kt", "fun test2() {}")
      }
    }
    frameworkLesson("lesson2") {
      eduTask("task1") {
        taskFile("src/Task.kt", "fun foo() {}")
        taskFile("src/Baz.kt", "fun baz() {}")
        taskFile("test/Tests1.kt", "fun test1() {}")
      }
    }
  }
}