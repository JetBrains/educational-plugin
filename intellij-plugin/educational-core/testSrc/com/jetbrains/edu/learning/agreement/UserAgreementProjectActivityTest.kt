package com.jetbrains.edu.learning.agreement

import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.application
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.agreement.UserAgreementSettings.UserAgreementProperties
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.api.UserAgreement
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import com.jetbrains.edu.learning.submissions.UserAgreementState.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.justRun
import io.mockk.verify
import org.junit.Test

class UserAgreementProjectActivityTest : CourseGenerationTestBase<EmptyProjectSettings>() {
  override val defaultSettings: EmptyProjectSettings
    get() = EmptyProjectSettings

  private lateinit var submissionsConnector: MarketplaceSubmissionsConnector

  @Volatile
  private var activityFinished: Boolean = true

  override fun setUp() {
    super.setUp()
    submissionsConnector = mockService<MarketplaceSubmissionsConnector>(application)

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
    coEvery { submissionsConnector.getUserAgreement() } returns Ok(UserAgreement(NOT_SHOWN, NOT_SHOWN))

    // when
    createCourseStructure(course)
    PlatformTestUtil.waitWhileBusy { !activityFinished }

    // then
    coVerify(exactly = 1) { submissionsConnector.getUserAgreement() }
    verify(exactly = 1) { userAgreementManager.showUserAgreement(any()) }
  }

  @Test
  fun `show user agreement dialog for the first time with existing remote state`() {
    // given
    val course = course {}

    val userAgreementManager = mockService<UserAgreementManager>(application)
    justRun { userAgreementManager.showUserAgreement(any()) }
    coEvery { submissionsConnector.getUserAgreement() } returns Ok(UserAgreement(ACCEPTED, DECLINED))

    // when
    createCourseStructure(course)
    PlatformTestUtil.waitWhileBusy { !activityFinished }

    // then
    assertEquals(
      UserAgreementProperties(ACCEPTED, DECLINED, isChangedByUser = false),
      UserAgreementSettings.getInstance().userAgreementProperties.value
    )
    coVerify(exactly = 1) { submissionsConnector.getUserAgreement() }
    verify(exactly = 0) { userAgreementManager.showUserAgreement(any()) }
  }

  @Test
  fun `handle error while loading remote state`() {
    // given
    val course = course {}

    val userAgreementManager = mockService<UserAgreementManager>(application)
    justRun { userAgreementManager.showUserAgreement(any()) }
    coEvery { submissionsConnector.getUserAgreement() } returns Err("Unexpected IO error")

    // when
    createCourseStructure(course)
    PlatformTestUtil.waitWhileBusy { !activityFinished }

    // then
    coVerify(exactly = 1) { submissionsConnector.getUserAgreement() }
    verify(exactly = 1) { userAgreementManager.showUserAgreement(any()) }
  }

  @Test
  fun `do not show user agreement dialog if it is already accepted`() {
    // given
    val course = course {}

    UserAgreementSettings.getInstance().updatePluginAgreementState(UserAgreementProperties.fullyAccepted())
    val userAgreementManager = mockService<UserAgreementManager>(application)
    justRun { userAgreementManager.showUserAgreement(any()) }
    coEvery { submissionsConnector.getUserAgreement() } returns Ok(UserAgreement(ACCEPTED, DECLINED))

    // when
    createCourseStructure(course)
    PlatformTestUtil.waitWhileBusy { !activityFinished }

    // then
    coVerify(exactly = 0) { submissionsConnector.getUserAgreement() }
    verify(exactly = 0) { userAgreementManager.showUserAgreement(any()) }
  }

  @Test
  fun `do not show user agreement dialog if it is already declined`() {
    // given
    val course = course {}

    UserAgreementSettings.getInstance().updatePluginAgreementState(UserAgreementProperties.declined())
    val userAgreementManager = mockService<UserAgreementManager>(application)
    justRun { userAgreementManager.showUserAgreement(any()) }
    coEvery { submissionsConnector.getUserAgreement() } returns Ok(UserAgreement(ACCEPTED, DECLINED))

    // when
    createCourseStructure(course)
    PlatformTestUtil.waitWhileBusy { !activityFinished }

    // then
    coVerify(exactly = 0) { submissionsConnector.getUserAgreement() }
    verify(exactly = 0) { userAgreementManager.showUserAgreement(any()) }
  }

  @Test
  fun `do not load remote state if it was reset`() {
    // given
    val course = course {}

    UserAgreementSettings.getInstance().updatePluginAgreementState(UserAgreementProperties())
    val userAgreementManager = mockService<UserAgreementManager>(application)
    justRun { userAgreementManager.showUserAgreement(any()) }
    coEvery { submissionsConnector.getUserAgreement() } returns Ok(UserAgreement(ACCEPTED, DECLINED))

    // when
    createCourseStructure(course)
    PlatformTestUtil.waitWhileBusy { !activityFinished }

    // then
    coVerify(exactly = 0) { submissionsConnector.getUserAgreement() }
    verify(exactly = 1) { userAgreementManager.showUserAgreement(any()) }
  }
}
