package com.jetbrains.edu.learning.taskDescription

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.action.LeaveFeedbackAction
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task


class TaskFeedbackLinksTest : EduTestCase() {

  fun testStepikLink() {
    if (ApplicationInfo.getInstance().build >= BUILD_231) {
      return
    }
    val task = getRemoteEduTask()
    assertEquals("Incorrect link", "https://release.stepik.org/lesson/0/step/1", LeaveFeedbackAction.getLink(task))
  }

  fun testNoneLink() {
    if (ApplicationInfo.getInstance().build >= BUILD_231) {
      return
    }
    val task = getRemoteEduTask(isMarketplaceCourse = true)
    assertNull(LeaveFeedbackAction.getLink(task))
  }

  fun testCustomLink() {
    if (ApplicationInfo.getInstance().build >= BUILD_231) {
      return
    }
    val task = getRemoteEduTask()
    task.feedbackLink = "https://www.jetbrains.com/"
    assertEquals("Incorrect link", "https://www.jetbrains.com/", LeaveFeedbackAction.getLink(task))
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

  companion object {
    private val BUILD_231: BuildNumber = BuildNumber.fromString("231")!!
  }
}