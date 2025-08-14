package com.jetbrains.edu.learning.agreement

import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.application
import com.jetbrains.edu.learning.agreement.UserAgreementSettings.AgreementStateResponse
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import com.jetbrains.edu.learning.submissions.UserAgreementState.ACCEPTED
import com.jetbrains.edu.learning.submissions.UserAgreementState.DECLINED
import io.mockk.justRun
import io.mockk.verify
import org.junit.Test

class UserAgreementProjectActivityTest : CourseGenerationTestBase<EmptyProjectSettings>() {
  override val defaultSettings: EmptyProjectSettings
    get() = EmptyProjectSettings

  @Volatile
  private var activityFinished: Boolean = true

  override fun setUp() {
    super.setUp()
    UserAgreementProjectActivity.enableActivityInTests(testRootDisposable) {
      activityFinished = true
    }
  }

  @Test
  fun `show user agreement dialog for the first time`() {
    // given
    val course = course {}

    val userAgreementManager = mockService<UserAgreementManager>(application)
    justRun { userAgreementManager.showUserAgreement(any()) }

    // when
    createCourseStructure(course)
    PlatformTestUtil.waitWhileBusy { !activityFinished }

    // then
    verify(exactly = 1) { userAgreementManager.showUserAgreement(any()) }
  }

  @Test
  fun `do not show user agreement dialog if it is already accepted`() {
    // given
    val course = course {}

    UserAgreementSettings.getInstance().setAgreementState(AgreementStateResponse(pluginAgreement = ACCEPTED, aiAgreement = ACCEPTED))
    val userAgreementManager = mockService<UserAgreementManager>(application)
    justRun { userAgreementManager.showUserAgreement(any()) }

    // when
    createCourseStructure(course)
    PlatformTestUtil.waitWhileBusy { !activityFinished }

    // then
    verify(exactly = 0) { userAgreementManager.showUserAgreement(any()) }
  }

  @Test
  fun `do not show user agreement dialog if it is already declined`() {
    // given
    val course = course {}

    UserAgreementSettings.getInstance().setAgreementState(AgreementStateResponse(DECLINED, DECLINED))
    val userAgreementManager = mockService<UserAgreementManager>(application)
    justRun { userAgreementManager.showUserAgreement(any()) }

    // when
    createCourseStructure(course)
    PlatformTestUtil.waitWhileBusy { !activityFinished }

    // then
    verify(exactly = 0) { userAgreementManager.showUserAgreement(any()) }
  }
}
