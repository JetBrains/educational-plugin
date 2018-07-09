package com.jetbrains.edu.learning.taskDescription

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepikUtils


class FeedbackLinksTest : EduTestCase() {

  fun testStepikLink() {
    val task = getRemoteEduTask()
    assertEquals("Incorrect link", "https://stepik.org/lesson/0/step/1", StepikUtils.getLink(task, 1))
  }

  fun testNoneLink() {
    val task = getRemoteEduTask()
    val feedbackLink = FeedbackLink()
    feedbackLink.type = FeedbackLink.LinkType.NONE
    task.feedbackLink = feedbackLink
    assertEquals("Incorrect link", null, StepikUtils.getLink(task, 1))
  }

  fun testCustomLink() {
    val task = getRemoteEduTask()
    val feedbackLink = FeedbackLink()
    feedbackLink.type = FeedbackLink.LinkType.CUSTOM
    feedbackLink.link = "https://www.jetbrains.com/"
    task.feedbackLink = feedbackLink
    assertEquals("Incorrect link", "https://www.jetbrains.com/", StepikUtils.getLink(task, 1))
  }

  private fun getRemoteEduTask(): Task {
    val course = course {
      lesson {
        eduTask { }
      }
    }
    val remoteCourse = RemoteCourse()
    remoteCourse.items = course.items
    remoteCourse.init(null, null, false)

    return (remoteCourse.items[0] as Lesson).getTaskList()[0]
  }
}