package com.jetbrains.edu.remote

import com.jetbrains.edu.learning.NotificationsTestBase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.marketplace.loginFakeMarketplaceUser
import com.jetbrains.edu.learning.navigation.NavigationUtils
import org.junit.Test

class UserAgreementNotificationTest : NotificationsTestBase() {
  override fun setUp() {
    super.setUp()
    loginFakeMarketplaceUser()
  }

  @Test
  fun `test update course notification not shown`() {
    val virtualFile = NavigationUtils.getFirstTask(createTestCourse())!!.getTaskFile("Task.txt")!!.getVirtualFile(project)!!
    checkNoEditorNotification<UserAgreementNotificationProvider>(virtualFile)
  }

  private fun createTestCourse(): EduCourse {
    return courseWithFiles {
      lesson {
        eduTask("task1") {
          taskFile("Task.txt")
        }
      }
    }.asRemote(CourseMode.STUDENT).apply { isMarketplace = true }
  }
}