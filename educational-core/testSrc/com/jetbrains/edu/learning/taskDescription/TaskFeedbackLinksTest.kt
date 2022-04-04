package com.jetbrains.edu.learning.taskDescription

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.actions.LeaveCommentAction
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task


class TaskFeedbackLinksTest : EduTestCase() {

  fun testStepikLink() {
    val task = getRemoteEduTask()
    assertEquals("Incorrect link", "https://release.stepik.org/lesson/0/step/1", LeaveCommentAction.getLink(task))
  }

  fun testNoneLink() {
    val task = getRemoteEduTask(isMarketplaceCourse = true)
    assertNull(LeaveCommentAction.getLink(task))
  }

  fun testCustomLink() {
    val task = getRemoteEduTask()
    task.feedbackLink = "https://www.jetbrains.com/"
    assertEquals("Incorrect link", "https://www.jetbrains.com/", LeaveCommentAction.getLink(task))
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