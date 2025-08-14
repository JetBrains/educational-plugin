package com.jetbrains.edu.learning.agreement

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.ProjectManager
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
import com.jetbrains.edu.learning.submissions.UserAgreementState.*
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
  private lateinit var projectManager: ProjectManager

  private lateinit var disposable: Disposable

  override fun setUp() {
    super.setUp()
    submissionsConnector = mockService<MarketplaceSubmissionsConnector>(application)
    coEvery { submissionsConnector.updateUserAgreements(any(), any()) } returns Ok(Unit)
    coEvery { submissionsConnector.changeSharingPreference(any()) } returns Ok(Response.success(Unit))
    coEvery { submissionsConnector.submitAgreementAcceptanceAnonymously(any()) } returns Unit
    // It's important to replace the project manager service back first since it's used in cleaning up test case state.
    // That's why we use a separate disposable here instead of `testRootDisposable`
    disposable = Disposer.newDisposable()
    projectManager = mockService<ProjectManager>(application, disposable)
    justRun { projectManager.reloadProject(any()) }

    courseWithFiles(createYamlConfigs = true) {}
  }

  override fun tearDown() {
    try {
      Disposer.dispose(disposable)
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
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
    verify(exactly = 0) { projectManager.reloadProject(project) }
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
    verify(exactly = 0) { projectManager.reloadProject(project) }
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
    verify(exactly = 1) { projectManager.reloadProject(project) }
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
    verify(exactly = 0) { projectManager.reloadProject(project) }
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
    verify(exactly = 0) { projectManager.reloadProject(project) }
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
    verify(exactly = 1) { projectManager.reloadProject(project) }
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
    verify(exactly = 1) { projectManager.reloadProject(project) }

    // when
    updatePluginAgreementState(UserAgreementProperties.pluginAgreementAccepted())

    // then
    coVerify(exactly = 1) { submissionsConnector.updateUserAgreements(ACCEPTED, DECLINED) }
    coVerify(exactly = 0) { submissionsConnector.changeSharingPreference(any()) }
    coVerify(exactly = 1) { submissionsConnector.submitAgreementAcceptanceAnonymously(isLoggedIn = true) }
    verify(exactly = 2) { projectManager.reloadProject(project) }
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
    verify(exactly = 1) { projectManager.reloadProject(project) }

    // when
    updatePluginAgreementState(UserAgreementProperties.pluginAgreementAccepted())

    // then
    coVerify(exactly = 0) { submissionsConnector.updateUserAgreements(any(), any()) }
    coVerify(exactly = 0) { submissionsConnector.changeSharingPreference(any()) }
    coVerify(exactly = 1) { submissionsConnector.submitAgreementAcceptanceAnonymously(isLoggedIn = false) }
    verify(exactly = 2) { projectManager.reloadProject(project) }
  }

  @Test
  fun `test plugin agreement is reset`() = runTest {
    // given
    mockJBAccount(testRootDisposable)
    initUserAgreementManager()

    // when
    updatePluginAgreementState(UserAgreementProperties.pluginAgreementAccepted())

    // then
    coVerify(exactly = 1) { submissionsConnector.updateUserAgreements(ACCEPTED, DECLINED) }
    coVerify(exactly = 0) { submissionsConnector.changeSharingPreference(any()) }
    coVerify(exactly = 1) { submissionsConnector.submitAgreementAcceptanceAnonymously(true) }
    verify(exactly = 0) { projectManager.reloadProject(project) }

    // when
    UserAgreementSettings.getInstance().resetUserAgreementSettings()
    advanceUntilIdle()

    // then
    coVerify(ordering = Ordering.ORDERED) {
      submissionsConnector.updateUserAgreements(ACCEPTED, DECLINED)
      submissionsConnector.updateUserAgreements(NOT_SHOWN, NOT_SHOWN)
    }
    coVerify(exactly = 0) { submissionsConnector.changeSharingPreference(any()) }
    coVerify(exactly = 1) { submissionsConnector.submitAgreementAcceptanceAnonymously(true) }
    verify(exactly = 1) { projectManager.reloadProject(project) }
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
