package com.jetbrains.edu.learning.taskDescription

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.actions.GoToTaskUrlAction
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task


class FeedbackLinksTest : EduTestCase() {

  fun testStepikLink() {
    val task = getRemoteEduTask()
    assertEquals("Incorrect link", "https://release.stepik.org/lesson/0/step/1", GoToTaskUrlAction.getLink(task))
  }

  fun testNoneLink() {
    val task = getRemoteEduTask()
    val feedbackLink = FeedbackLink()
    feedbackLink.type = FeedbackLink.LinkType.NONE
    task.feedbackLink = feedbackLink
    try {
      println("link = ${GoToTaskUrlAction.getLink(task)}")
      fail("Exception expected to be thrown")
    }
    catch (e: IllegalStateException) {
      //exception thrown
    }
  }

  fun testCustomLink() {
    val task = getRemoteEduTask()
    val feedbackLink = FeedbackLink()
    feedbackLink.type = FeedbackLink.LinkType.CUSTOM
    feedbackLink.link = "https://www.jetbrains.com/"
    task.feedbackLink = feedbackLink
    assertEquals("Incorrect link", "https://www.jetbrains.com/", GoToTaskUrlAction.getLink(task))
  }

  private fun getRemoteEduTask(): Task {
    val course = course {
      lesson {
        eduTask { }
      }
    }
    val remoteCourse = EduCourse()
    remoteCourse.items = course.items
    remoteCourse.init(null, null, false)

    return (remoteCourse.items[0] as Lesson).taskList[0]
  }
}