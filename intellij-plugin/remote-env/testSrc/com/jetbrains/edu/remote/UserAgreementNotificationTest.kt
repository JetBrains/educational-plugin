package com.jetbrains.edu.remote

import com.jetbrains.edu.learning.NotificationsTestBase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.marketplace.api.SubmissionsService
import com.jetbrains.edu.learning.marketplace.loginFakeMarketplaceUser
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.submissions.UserAgreementState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit

class UserAgreementNotificationTest : NotificationsTestBase() {
  override fun setUp() {
    super.setUp()
    loginFakeMarketplaceUser()
  }

  fun `test update course notification not shown`() {
    configureAgreementStateResponse()
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

  private fun configureAgreementStateResponse() {
    mockkConstructor(Retrofit::class)
    val service = mockk<SubmissionsService>()
    every {
      anyConstructed<Retrofit>().create(SubmissionsService::class.java)
    } returns service

    val userAgreementStateCall = mockk<Call<ResponseBody>>()
    coEvery { service.getUserAgreementState() } returns userAgreementStateCall
    every { userAgreementStateCall.execute() } returns Response.success(UserAgreementState.NOT_SHOWN.toString().toResponseBody("application/json; charset=UTF-8".toMediaType()))
  }
}