package com.jetbrains.edu.learning.update

import com.jetbrains.edu.learning.NotificationsTestBase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.navigation.NavigationUtils.getFirstTask
import com.jetbrains.edu.learning.stepik.UpdateCourseNotificationProvider
import org.junit.Test

class UpdateCourseNotificationProviderTest : NotificationsTestBase() {

  @Test
  fun `test update course notification shown`() {
    val course = createCourse(isUpToDate = false)
    val virtualFile = getFirstTask(course)!!.getTaskFile("Task.txt")!!.getVirtualFile(project)!!
    checkEditorNotification<UpdateCourseNotificationProvider>(virtualFile)
  }

  @Test
  fun `test update course notification not shown`() {
    val course = createCourse(isUpToDate = true)
    val virtualFile = getFirstTask(course)!!.getTaskFile("Task.txt")!!.getVirtualFile(project)!!
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
    course.isUpToDate = isUpToDate
    return course
  }
}
