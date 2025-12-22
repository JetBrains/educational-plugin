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
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test

class UserAgreementProjectActivityTest : CourseGenerationTestBase<EmptyProjectSettings>() {
  override val defaultSettings: EmptyProjectSettings
    get() = EmptyProjectSettings

  private lateinit var submissionsConnector: MarketplaceSubmissionsConnector
  private lateinit var userAgreementSettings: UserAgreementSettings
  private lateinit var userAgreementManager: UserAgreementManager

  @Volatile
  private var activityFinished: Boolean = true

  override fun setUp() {
    super.setUp()
    submissionsConnector = mockService<MarketplaceSubmissionsConnector>(application)
    coEvery { submissionsConnector.updateUserAgreements(any(), any()) } returns Ok(Unit)

    userAgreementSettings = mockService<UserAgreementSettings>(application)
    every { userAgreementSettings.updatePluginAgreementState(any(), any()) } returns Unit

    userAgreementManager = mockService<UserAgreementManager>(application)
    justRun { userAgreementManager.showUserAgreement(any()) }

    UserAgreementProjectActivity.enableActivityInTests(testRootDisposable) {
      activityFinished = true
    }
  }

  @Test
  fun `show user agreement dialog for the first time`() {
    // given
    val course = course {}

    every { userAgreementSettings.userAgreementProperties } returns MutableStateFlow(UserAgreementProperties(isChangedByUser = false))
    coEvery { submissionsConnector.getUserAgreement() } returns Ok(UserAgreement(NOT_SHOWN, NOT_SHOWN))

    // when
    createCourseStructure(course)
    PlatformTestUtil.waitWhileBusy { !activityFinished }

    // then
    coVerify(exactly = 1) { submissionsConnector.getUserAgreement() }
    coVerify(exactly = 0) { submissionsConnector.updateUserAgreements(any(), any()) }
    verify(exactly = 1) { userAgreementManager.showUserAgreement(any()) }
    // Since we mock showing the user agreement dialog, nothing should update the local agreement state
    verify(exactly = 0) { userAgreementSettings.updatePluginAgreementState(any(), any()) }
  }

  @Test
  fun `show user agreement dialog for the first time with existing remote state`() {
    // given
    val course = course {}

    every { userAgreementSettings.userAgreementProperties } returns MutableStateFlow(UserAgreementProperties(isChangedByUser = false))
    coEvery { submissionsConnector.getUserAgreement() } returns Ok(UserAgreement(ACCEPTED, DECLINED))

    // when
    createCourseStructure(course)
    PlatformTestUtil.waitWhileBusy { !activityFinished }

    // then
    coVerify(exactly = 1) { submissionsConnector.getUserAgreement() }
    coVerify(exactly = 0) { submissionsConnector.updateUserAgreements(any(), any()) }
    verify(exactly = 0) { userAgreementManager.showUserAgreement(any()) }
    verify(exactly = 1) { userAgreementSettings.updatePluginAgreementState(UserAgreementProperties(ACCEPTED, DECLINED, isChangedByUser = false), any()) }
  }

  @Test
  fun `handle error while loading remote state without local state`() {
    // given
    val course = course {}

    every { userAgreementSettings.userAgreementProperties } returns MutableStateFlow(UserAgreementProperties(isChangedByUser = false))
    coEvery { submissionsConnector.getUserAgreement() } returns Err("Unexpected IO error")

    // when
    createCourseStructure(course)
    PlatformTestUtil.waitWhileBusy { !activityFinished }

    // then
    coVerify(exactly = 1) { submissionsConnector.getUserAgreement() }
    coVerify(exactly = 0) { submissionsConnector.updateUserAgreements(any(), any()) }
    verify(exactly = 1) { userAgreementManager.showUserAgreement(any()) }
    // Since we mock showing the user agreement dialog, nothing should update the local agreement state
    verify(exactly = 0) { userAgreementSettings.updatePluginAgreementState(any(), any()) }
  }

  @Test
  fun `do not show user agreement dialog if it is already accepted`() {
    // given
    val course = course {}

    every { userAgreementSettings.userAgreementProperties } returns MutableStateFlow(UserAgreementProperties.fullyAccepted())
    coEvery { submissionsConnector.getUserAgreement() } returns Ok(UserAgreement(ACCEPTED, ACCEPTED))

    // when
    createCourseStructure(course)
    PlatformTestUtil.waitWhileBusy { !activityFinished }

    // then
    coVerify(exactly = 1) { submissionsConnector.getUserAgreement() }
    coVerify(exactly = 0) { submissionsConnector.updateUserAgreements(any(), any()) }
    verify(exactly = 0) { userAgreementManager.showUserAgreement(any()) }
    verify(exactly = 0) { userAgreementSettings.updatePluginAgreementState(any(), any()) }
  }

  @Test
  fun `do not show user agreement dialog if it is already declined`() {
    // given
    val course = course {}

    every { userAgreementSettings.userAgreementProperties } returns MutableStateFlow(UserAgreementProperties.declined())
    coEvery { submissionsConnector.getUserAgreement() } returns Ok(UserAgreement(ACCEPTED, DECLINED))

    // when
    createCourseStructure(course)
    PlatformTestUtil.waitWhileBusy { !activityFinished }

    // then
    coVerify(exactly = 0) { submissionsConnector.getUserAgreement() }
    coVerify(exactly = 0) { submissionsConnector.updateUserAgreements(any(), any()) }
    verify(exactly = 0) { userAgreementManager.showUserAgreement(any()) }
    verify(exactly = 0) { userAgreementSettings.updatePluginAgreementState(any(), any()) }
  }

  @Test
  fun `do not apply remote state if it was reset`() {
    // given
    val course = course {}

    every { userAgreementSettings.userAgreementProperties } returns MutableStateFlow(UserAgreementProperties())
    coEvery { submissionsConnector.getUserAgreement() } returns Ok(UserAgreement(ACCEPTED, DECLINED))

    // when
    createCourseStructure(course)
    PlatformTestUtil.waitWhileBusy { !activityFinished }

    // then
    coVerify(exactly = 1) { submissionsConnector.getUserAgreement() }
    coVerify(exactly = 0) { submissionsConnector.updateUserAgreements(any(), any()) }
    verify(exactly = 1) { userAgreementManager.showUserAgreement(any()) }
    // Since we mock showing the user agreement dialog, nothing should update the local agreement state
    verify(exactly = 0) { userAgreementSettings.updatePluginAgreementState(any(), any()) }
  }

  @Test
  fun `update remote state if local is accepted and unsynced with remote`() {
    // given
    val course = course {}

    every { userAgreementSettings.userAgreementProperties } returns MutableStateFlow(UserAgreementProperties.fullyAccepted())
    coEvery { submissionsConnector.getUserAgreement() } returns Ok(UserAgreement(NOT_SHOWN, NOT_SHOWN))

    // when
    createCourseStructure(course)
    PlatformTestUtil.waitWhileBusy { !activityFinished }

    // then
    coVerify(exactly = 1) { submissionsConnector.getUserAgreement() }
    coVerify(exactly = 1) { submissionsConnector.updateUserAgreements(ACCEPTED, ACCEPTED) }
    verify(exactly = 0) { userAgreementManager.showUserAgreement(any()) }
    verify(exactly = 0) { userAgreementSettings.updatePluginAgreementState(any(), any()) }
  }

  @Test
  fun `update remote state if local is accepted and remote state is unknown`() {
    // given
    val course = course {}

    every { userAgreementSettings.userAgreementProperties } returns MutableStateFlow(UserAgreementProperties.fullyAccepted())
    coEvery { submissionsConnector.getUserAgreement() } returns Err("Unexpected IO error")

    // when
    createCourseStructure(course)
    PlatformTestUtil.waitWhileBusy { !activityFinished }

    // then
    coVerify(exactly = 1) { submissionsConnector.getUserAgreement() }
    coVerify(exactly = 1) { submissionsConnector.updateUserAgreements(ACCEPTED, ACCEPTED) }
    verify(exactly = 0) { userAgreementManager.showUserAgreement(any()) }
    verify(exactly = 0) { userAgreementSettings.updatePluginAgreementState(any(), any()) }
  }
}
