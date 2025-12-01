package com.jetbrains.edu.socialMedia.marketplace

import com.jetbrains.edu.learning.LessonBuilder
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.socialMedia.SocialMediaPostActionTestBase
import io.mockk.verify
import org.junit.Test

class MarketplacePostToSocialMediaTest : SocialMediaPostActionTestBase() {

  @Test
  fun `test post to social media after last task solved`() {
    // given
    val course = createEduCourse()
    val currentTask = course.findTask("lesson1", "task5")
    // Mark all tasks except the last one as solved
    course.allTasks.take(TASK_COUNT - 1).forEach { it.status = CheckStatus.Solved }

    // when
    val isDialogShown = launchCheckAction(currentTask)

    // then
    assertTrue(isDialogShown)

    verify(exactly = 1) { mockXConnector.tweet(any(), isNull(inverse = true)) }
    verify(exactly = 1) { mockLinkedInConnector.createPostWithMedia(any(), any(), any()) }
  }

  @Test
  fun `test post to social media after last task solved (framework lesson)`() {
    // given
    val course = createEduCourse(lessonType = LessonType.FRAMEWORK)
    val currentTask = course.findTask("lesson1", "task5")
    // Mark all tasks except the last one as solved
    course.allTasks.take(TASK_COUNT - 1).forEach { it.status = CheckStatus.Solved }

    // when
    val isDialogShown = launchCheckAction(currentTask)

    // then
    assertTrue(isDialogShown)

    verify(exactly = 1) { mockXConnector.tweet(any(), isNull(inverse = true)) }
    verify(exactly = 1) { mockLinkedInConnector.createPostWithMedia(any(), any(), any()) }
  }

  @Test
  fun `test post to social media after reaching task solved limit`() {
    // given
    val course = createEduCourse()
    val currentTask = course.findTask("lesson1", "task4")
    // Mark enough tasks as solved to show achievement dialog after next solved task
    course.allTasks.take(POSTING_THRESHOLD_TASK_COUNT - 1).forEach { it.status = CheckStatus.Solved }

    // when
    val isDialogShown = launchCheckAction(currentTask)

    // then
    assertTrue(isDialogShown)

    verify(exactly = 1) { mockXConnector.tweet(any(), isNull(inverse = true)) }
    verify(exactly = 1) { mockLinkedInConnector.createPostWithMedia(any(), any(), any()) }
  }

  @Test
  fun `test post to social media after reaching task solved limit (framework lesson)`() {
    // given
    val course = createEduCourse(lessonType = LessonType.FRAMEWORK)
    val currentTask = course.findTask("lesson1", "task4")
    // Mark enough tasks as solved to show achievement dialog after next solved task
    course.allTasks.take(POSTING_THRESHOLD_TASK_COUNT - 1).forEach { it.status = CheckStatus.Solved }

    // when
    val isDialogShown = launchCheckAction(currentTask)

    // then
    assertTrue(isDialogShown)

    verify(exactly = 1) { mockXConnector.tweet(any(), isNull(inverse = true)) }
    verify(exactly = 1) { mockLinkedInConnector.createPostWithMedia(any(), any(), any()) }
  }

  @Test
  fun `test do not post for course from course storage`() {
    // given
    val course = createEduCourse(id = 200001)
    val currentTask = course.findTask("lesson1", "task5")
    // Mark all tasks except the last one as solved
    course.allTasks.take(TASK_COUNT - 1).forEach { it.status = CheckStatus.Solved }

    // when
    val isDialogShown = launchCheckAction(currentTask)

    // then
    assertFalse(isDialogShown)

    verify(exactly = 0) { mockXConnector.tweet(any(), any()) }
    verify(exactly = 0) { mockLinkedInConnector.createPostWithMedia(any(), any(), any()) }
  }

  @Test
  fun `test do not post if not enough tasks solved`() {
    // given
    val course = createEduCourse()
    val currentTask = course.findTask("lesson1", "task4")
    // Mark that number of tasks as solved not to have enough solved tasks
    // after success task checking to show the dialog
    course.allTasks.take(POSTING_THRESHOLD_TASK_COUNT - 2).forEach { it.status = CheckStatus.Solved }

    // when
    val isDialogShown = launchCheckAction(currentTask)

    // then
    assertFalse(isDialogShown)

    verify(exactly = 0) { mockXConnector.tweet(any(), any()) }
    verify(exactly = 0) { mockLinkedInConnector.createPostWithMedia(any(), any(), any()) }
  }

  @Test
  fun `test do not post if not enough tasks solved (framework lesson)`() {
    // given
    val course = createEduCourse(lessonType = LessonType.FRAMEWORK)
    val currentTask = course.findTask("lesson1", "task3")
    // Mark that number of tasks as solved not to have enough solved tasks
    // after success task checking to show the dialog
    course.allTasks.take(POSTING_THRESHOLD_TASK_COUNT - 2).forEach { it.status = CheckStatus.Solved }

    // when
    val isDialogShown = launchCheckAction(currentTask)

    // then
    assertFalse(isDialogShown)

    verify(exactly = 0) { mockXConnector.tweet(any(), any()) }
    verify(exactly = 0) { mockLinkedInConnector.createPostWithMedia(any(), any(), any()) }
  }

  private fun createEduCourse(
    id: Int = 123,
    courseMode: CourseMode = CourseMode.STUDENT,
    lessonType: LessonType = LessonType.EDU
  ): Course {
    fun <T : Lesson> LessonBuilder<T>.createTasks() {
      for (i in 1..TASK_COUNT) {
        eduTask("task$i") {
          taskFile("taskFile$i.txt")
        }
      }
    }

    val course = courseWithFiles(id = id, courseMode = courseMode) {
      when (lessonType) {
        LessonType.EDU -> {
          lesson("lesson1") {
            createTasks()
          }
        }
        LessonType.FRAMEWORK -> {
          frameworkLesson("lesson1") {
            createTasks()
          }
        }
      }
    }
    course.isMarketplace = true
    return course
  }

  companion object {
    private const val TASK_COUNT = 5
    private const val POSTING_THRESHOLD_TASK_COUNT = 4
  }

  private enum class LessonType {
    EDU, FRAMEWORK
  }
}
