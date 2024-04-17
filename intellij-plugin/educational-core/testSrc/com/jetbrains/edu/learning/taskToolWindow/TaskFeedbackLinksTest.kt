package com.jetbrains.edu.learning.taskToolWindow

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.junit.Test


class TaskFeedbackLinksTest : EduTestCase() {

  @Test
  fun testNoneLink() {
    val task = getRemoteEduTask(isMarketplaceCourse = true)
    assertNull(task.feedbackLink)
  }

  @Test
  fun testCustomLink() {
    val task = getRemoteEduTask()
    task.feedbackLink = "https://www.jetbrains.com/"
    assertEquals("Incorrect link", "https://www.jetbrains.com/", task.feedbackLink)
  }

  private fun getRemoteEduTask(isMarketplaceCourse: Boolean = false): Task {
    val course = course {
      lesson {
        eduTask { }
      }
    }
    val remoteCourse = EduCourse().apply {
      id = 1
      isMarketplace = isMarketplaceCourse
    }
    remoteCourse.items = course.items
    remoteCourse.init(false)

    return (remoteCourse.items[0] as Lesson).taskList[0]
  }
}