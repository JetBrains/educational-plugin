package com.jetbrains.edu.learning.agreement

import com.intellij.util.application
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.mockJBAccount
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.submissions.UserAgreementState
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.Test
import retrofit2.Response

class UserAgreementManagerTest : EduTestCase() {
  override fun setUp() {
    super.setUp()
    /**
     * We test UserAgreementManager that reactively executes actions (like network calls) on a changes in UserAgreementSettings.
     * Thus, we want this service to be initialized before any of the following tests are executed.
     */
    UserAgreementManager.getInstance()
    val mockedService = mockService<MarketplaceSubmissionsConnector>(application)
    coEvery { mockedService.updateUserAgreements(any(), any()) } returns Ok(Unit)
    coEvery { mockedService.changeSharingPreference(any()) } returns Ok(Response.success(Unit))
    coEvery { mockedService.submitAgreementAcceptanceAnonymously(any()) } returns Unit
  }

  @Test
  fun `test plugin agreement accepted (ai declined)`() {
    // when user logged in
    mockJBAccount(testRootDisposable)
    // and
    UserAgreementSettings.getInstance().setAgreementState(
      UserAgreementSettings.AgreementStateResponse(
        UserAgreementState.ACCEPTED,
        UserAgreementState.DECLINED
      )
    )

    val mockedService = MarketplaceSubmissionsConnector.getInstance()
    coVerify(exactly = 1) { mockedService.updateUserAgreements(UserAgreementState.ACCEPTED, UserAgreementState.DECLINED) }
    coVerify(exactly = 0) { MarketplaceSubmissionsConnector.getInstance().changeSharingPreference(any()) }
    coVerify(exactly = 1) { mockedService.submitAgreementAcceptanceAnonymously(true) }
  }

  @Test
  fun `test plugin & ai agreements are accepted`() {
    // when user logged in
    mockJBAccount(testRootDisposable)
    // and
    UserAgreementSettings.getInstance().setAgreementState(
      UserAgreementSettings.AgreementStateResponse(
        UserAgreementState.ACCEPTED,
        UserAgreementState.ACCEPTED
      )
    )

    val mockedService = MarketplaceSubmissionsConnector.getInstance()
    coVerify(exactly = 1) { mockedService.updateUserAgreements(UserAgreementState.ACCEPTED, UserAgreementState.ACCEPTED) }
    coVerify(exactly = 0) { MarketplaceSubmissionsConnector.getInstance().changeSharingPreference(any()) }
    coVerify(exactly = 1) { mockedService.submitAgreementAcceptanceAnonymously(true) }
  }

  @Test
  fun `test plugin & ai agreements are declined`() {
    // when user logged in
    mockJBAccount(testRootDisposable)
    // and
    UserAgreementSettings.getInstance().setAgreementState(
      UserAgreementSettings.AgreementStateResponse(
        UserAgreementState.DECLINED,
        UserAgreementState.DECLINED
      )
    )

    val mockedService = MarketplaceSubmissionsConnector.getInstance()
    coVerify(exactly = 1) { mockedService.updateUserAgreements(UserAgreementState.DECLINED, UserAgreementState.DECLINED) }
    coVerify(exactly = 0) { mockedService.changeSharingPreference(any()) }
    coVerify(exactly = 0) { mockedService.submitAgreementAcceptanceAnonymously(any()) }
  }

  @Test
  fun `test plugin agreement accepted (ai declined) when user is not logged in`() {
    // when
    UserAgreementSettings.getInstance().setAgreementState(
      UserAgreementSettings.AgreementStateResponse(
        UserAgreementState.ACCEPTED,
        UserAgreementState.DECLINED
      )
    )

    // then
    val mockedService = MarketplaceSubmissionsConnector.getInstance()
    coVerify(exactly = 0) { mockedService.updateUserAgreements(any(), any()) }
    coVerify(exactly = 0) { mockedService.changeSharingPreference(any()) }
    coVerify(exactly = 1) { mockedService.submitAgreementAcceptanceAnonymously(false) }
  }

  @Test
  fun `test plugin & ai agreements are accepted when user is not logged in`() {
    // when
    UserAgreementSettings.getInstance().setAgreementState(
      UserAgreementSettings.AgreementStateResponse(
        UserAgreementState.ACCEPTED,
        UserAgreementState.ACCEPTED
      )
    )

    // then
    val mockedService = MarketplaceSubmissionsConnector.getInstance()
    coVerify(exactly = 0) { mockedService.updateUserAgreements(any(), any()) }
    coVerify(exactly = 0) { mockedService.changeSharingPreference(any()) }
    coVerify(exactly = 1) { mockedService.submitAgreementAcceptanceAnonymously(false) }
  }

  @Test
  fun `test plugin & ai agreements are declined when user is not logged in`() {
    // when
    UserAgreementSettings.getInstance().setAgreementState(
      UserAgreementSettings.AgreementStateResponse(
        UserAgreementState.DECLINED,
        UserAgreementState.DECLINED
      )
    )

    // then
    val mockedService = MarketplaceSubmissionsConnector.getInstance()
    coVerify(exactly = 0) { mockedService.updateUserAgreements(any(), any()) }
    coVerify(exactly = 0) { mockedService.changeSharingPreference(any()) }
    coVerify(exactly = 0) { mockedService.submitAgreementAcceptanceAnonymously(any()) }
  }

  @Test
  fun `test plugin agreement declined and then accepted for logged in user`() {
    // when
    mockJBAccount(testRootDisposable)
    UserAgreementSettings.getInstance().setAgreementState(
      UserAgreementSettings.AgreementStateResponse(
        UserAgreementState.DECLINED,
        UserAgreementState.DECLINED
      )
    )

    // then
    val mockedService = MarketplaceSubmissionsConnector.getInstance()
    coVerify(exactly = 1) { mockedService.updateUserAgreements(UserAgreementState.DECLINED, UserAgreementState.DECLINED) }
    coVerify(exactly = 0) { mockedService.changeSharingPreference(any()) }
    coVerify(exactly = 0) { mockedService.submitAgreementAcceptanceAnonymously(any()) }

    // when
    UserAgreementSettings.getInstance().setAgreementState(
      UserAgreementSettings.AgreementStateResponse(
        UserAgreementState.ACCEPTED,
        UserAgreementState.DECLINED
      )
    )

    // then
    coVerify(exactly = 1) { mockedService.updateUserAgreements(UserAgreementState.ACCEPTED, UserAgreementState.DECLINED) }
    coVerify(exactly = 0) { mockedService.changeSharingPreference(any()) }
    coVerify(exactly = 1) { mockedService.submitAgreementAcceptanceAnonymously(isLoggedIn = true) }
  }

  @Test
  fun `test plugin agreement declined and then accepted for unlogged in user`() {
    // when
    UserAgreementSettings.getInstance().setAgreementState(
      UserAgreementSettings.AgreementStateResponse(
        UserAgreementState.DECLINED,
        UserAgreementState.DECLINED
      )
    )

    // then
    val mockedService = MarketplaceSubmissionsConnector.getInstance()
    coVerify(exactly = 0) { mockedService.updateUserAgreements(any(), any()) }
    coVerify(exactly = 0) { mockedService.changeSharingPreference(any()) }
    coVerify(exactly = 0) { mockedService.submitAgreementAcceptanceAnonymously(any()) }

    // when
    UserAgreementSettings.getInstance().setAgreementState(
      UserAgreementSettings.AgreementStateResponse(
        UserAgreementState.ACCEPTED,
        UserAgreementState.DECLINED
      )
    )

    // then
    coVerify(exactly = 0) { mockedService.updateUserAgreements(any(), any()) }
    coVerify(exactly = 0) { mockedService.changeSharingPreference(any()) }
    coVerify(exactly = 1) { mockedService.submitAgreementAcceptanceAnonymously(isLoggedIn = false) }
  }
}