package com.jetbrains.edu.learning.agreement

import com.intellij.openapi.util.Disposer
import com.intellij.platform.util.coroutines.childScope
import com.intellij.util.application
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.agreement.UserAgreementSettings.UserAgreementProperties
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.mockJBAccount
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.submissions.UserAgreementState.ACCEPTED
import com.jetbrains.edu.learning.submissions.UserAgreementState.DECLINED
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class UserAgreementManagerTest : EduTestCase() {

  private lateinit var submissionsConnector: MarketplaceSubmissionsConnector

  override fun setUp() {
    super.setUp()
    submissionsConnector = mockService<MarketplaceSubmissionsConnector>(application)
    coEvery { submissionsConnector.updateUserAgreements(any(), any()) } returns Ok(Unit)
    coEvery { submissionsConnector.changeSharingPreference(any()) } returns Ok(Response.success(Unit))
    coEvery { submissionsConnector.submitAgreementAcceptanceAnonymously(any()) } returns Unit
  }

  @Test
  fun `test plugin agreement accepted (ai declined)`() = runTest {
    // given
    mockJBAccount(testRootDisposable)
    initUserAgreementManager()

    // when
    updatePluginAgreementState(UserAgreementProperties.pluginAgreementAccepted())

    // then
    coVerify(exactly = 1) { submissionsConnector.updateUserAgreements(ACCEPTED, DECLINED) }
    coVerify(exactly = 0) { submissionsConnector.changeSharingPreference(any()) }
    coVerify(exactly = 1) { submissionsConnector.submitAgreementAcceptanceAnonymously(true) }
  }

  @Test
  fun `test plugin & ai agreements are accepted`() = runTest {
    // given
    mockJBAccount(testRootDisposable)
    initUserAgreementManager()

    // when
    updatePluginAgreementState(UserAgreementProperties.fullyAccepted())

    // then
    coVerify(exactly = 1) { submissionsConnector.updateUserAgreements(ACCEPTED, ACCEPTED) }
    coVerify(exactly = 0) { submissionsConnector.changeSharingPreference(any()) }
    coVerify(exactly = 1) { submissionsConnector.submitAgreementAcceptanceAnonymously(true) }
  }

  @Test
  fun `test plugin & ai agreements are declined`() = runTest {
    // given
    mockJBAccount(testRootDisposable)
    initUserAgreementManager()

    // when
    updatePluginAgreementState(UserAgreementProperties.declined())

    // then
    coVerify(exactly = 1) { submissionsConnector.updateUserAgreements(DECLINED, DECLINED) }
    coVerify(exactly = 0) { submissionsConnector.changeSharingPreference(any()) }
    coVerify(exactly = 0) { submissionsConnector.submitAgreementAcceptanceAnonymously(any()) }
  }

  @Test
  fun `test plugin agreement accepted (ai declined) when user is not logged in`() = runTest {
    // given
    mockNotLoggedInUser()
    initUserAgreementManager()

    // when
    updatePluginAgreementState(UserAgreementProperties.pluginAgreementAccepted())

    // then
    coVerify(exactly = 0) { submissionsConnector.updateUserAgreements(any(), any()) }
    coVerify(exactly = 0) { submissionsConnector.changeSharingPreference(any()) }
    coVerify(exactly = 1) { submissionsConnector.submitAgreementAcceptanceAnonymously(false) }
  }

  @Test
  fun `test plugin & ai agreements are accepted when user is not logged in`() = runTest {
    // given
    mockNotLoggedInUser()
    initUserAgreementManager()

    // when
    updatePluginAgreementState(UserAgreementProperties.fullyAccepted())

    // then
    coVerify(exactly = 0) { submissionsConnector.updateUserAgreements(any(), any()) }
    coVerify(exactly = 0) { submissionsConnector.changeSharingPreference(any()) }
    coVerify(exactly = 1) { submissionsConnector.submitAgreementAcceptanceAnonymously(false) }
  }

  @Test
  fun `test plugin & ai agreements are declined when user is not logged in`() = runTest {
    // given
    mockNotLoggedInUser()
    initUserAgreementManager()

    // when
    updatePluginAgreementState(UserAgreementProperties.declined())

    // then
    coVerify(exactly = 0) { submissionsConnector.updateUserAgreements(any(), any()) }
    coVerify(exactly = 0) { submissionsConnector.changeSharingPreference(any()) }
    coVerify(exactly = 0) { submissionsConnector.submitAgreementAcceptanceAnonymously(any()) }
  }

  @Test
  fun `test plugin agreement declined and then accepted for logged in user`() = runTest {
    // given
    mockJBAccount(testRootDisposable)
    initUserAgreementManager()

    // when
    updatePluginAgreementState(UserAgreementProperties.declined())

    // then
    coVerify(exactly = 1) { submissionsConnector.updateUserAgreements(DECLINED, DECLINED) }
    coVerify(exactly = 0) { submissionsConnector.changeSharingPreference(any()) }
    coVerify(exactly = 0) { submissionsConnector.submitAgreementAcceptanceAnonymously(any()) }

    // when
    updatePluginAgreementState(UserAgreementProperties.pluginAgreementAccepted())

    // then
    coVerify(exactly = 1) { submissionsConnector.updateUserAgreements(ACCEPTED, DECLINED) }
    coVerify(exactly = 0) { submissionsConnector.changeSharingPreference(any()) }
    coVerify(exactly = 1) { submissionsConnector.submitAgreementAcceptanceAnonymously(isLoggedIn = true) }
  }

  @Test
  fun `test plugin agreement declined and then accepted for unlogged in user`() = runTest {
    // given
    mockNotLoggedInUser()
    initUserAgreementManager()

    // when
    updatePluginAgreementState(UserAgreementProperties.declined())

    // then
    coVerify(exactly = 0) { submissionsConnector.updateUserAgreements(any(), any()) }
    coVerify(exactly = 0) { submissionsConnector.changeSharingPreference(any()) }
    coVerify(exactly = 0) { submissionsConnector.submitAgreementAcceptanceAnonymously(any()) }

    // when
    updatePluginAgreementState(UserAgreementProperties.pluginAgreementAccepted())

    // then
    coVerify(exactly = 0) { submissionsConnector.updateUserAgreements(any(), any()) }
    coVerify(exactly = 0) { submissionsConnector.changeSharingPreference(any()) }
    coVerify(exactly = 1) { submissionsConnector.submitAgreementAcceptanceAnonymously(isLoggedIn = false) }
  }

  @OptIn(DelicateCoroutinesApi::class)
  private fun TestScope.initUserAgreementManager() {
    val serviceScope = GlobalScope.childScope("UserAgreementManager-scope", Dispatchers.Unconfined)
    UserAgreementManager(serviceScope, StandardTestDispatcher(testScheduler))

    Disposer.register(testRootDisposable) {
      serviceScope.cancel()
    }
  }

  private fun TestScope.updatePluginAgreementState(newState: UserAgreementProperties) {
    UserAgreementSettings.getInstance().updatePluginAgreementState(newState)
    advanceUntilIdle()
  }

  /**
   * Mocks the behavior of [MarketplaceSettings.isJBALoggedIn] to return `false`.
   * It's necessary because even tests may return your actual login info
   */
  private fun mockNotLoggedInUser() {
    mockkObject(MarketplaceSettings)
    every { MarketplaceSettings.isJBALoggedIn() } returns false
    Disposer.register(testRootDisposable) {
      unmockkObject(MarketplaceSettings)
    }
  }
}
