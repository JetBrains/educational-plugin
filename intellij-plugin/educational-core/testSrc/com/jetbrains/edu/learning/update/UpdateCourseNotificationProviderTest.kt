package com.jetbrains.edu.learning.update

import com.jetbrains.edu.learning.NotificationsTestBase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduCourse
import org.junit.Test

class UpdateCourseNotificationProviderTest : NotificationsTestBase() {

  @Test
  fun `test update course notification shown`() {
    createCourse(isUpToDate = false)
    val virtualFile = findFile("lesson1/task1/Task.txt")
    checkEditorNotification<UpdateCourseNotificationProvider>(virtualFile)
  }

  @Test
  fun `test update course notification not shown`() {
    createCourse(isUpToDate = true)
    val virtualFile = findFile("lesson1/task1/Task.txt")
    checkNoEditorNotification<UpdateCourseNotificationProvider>(virtualFile)
  }

  private fun createCourse(isUpToDate: Boolean): EduCourse {
    val course = courseWithFiles {
      lesson {
        eduTask("task1") {
          taskFile("Task.txt")
        }
      }
    }.asRemote(CourseMode.STUDENT)
    course.isMarketplace = true
    course.isUpToDate = isUpToDate
    return course
  }
}
