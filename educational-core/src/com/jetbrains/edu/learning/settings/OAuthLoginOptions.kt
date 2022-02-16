package com.jetbrains.edu.learning.settings

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.util.Disposer
import com.intellij.ui.HyperlinkAdapter
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.api.EduOAuthConnector
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.AuthorizationPlace
import javax.swing.JComponent
import javax.swing.event.HyperlinkEvent

abstract class OAuthLoginOptions<T : OAuthAccount<out Any>> : LoginOptions<T>() {
  protected abstract val connector: EduOAuthConnector<T, *>
  private val disposable = Disposer.newDisposable()

  override fun createComponent(): JComponent? {
    val component = super.createComponent() ?: return null
    subscribeLogInListener()
    return component
  }

  override fun getCurrentAccount(): T? = connector.account

  override fun setCurrentAccount(account: T?) {
    connector.account = account
  }

  override fun disposeUIResources() = Disposer.dispose(disposable)

  override fun createAuthorizeListener(): HyperlinkAdapter = object : HyperlinkAdapter() {
    override fun hyperlinkActivated(event: HyperlinkEvent) = connector.doAuthorize(authorizationPlace = AuthorizationPlace.SETTINGS)
  }

  override fun createLogOutListener(): HyperlinkAdapter = object : HyperlinkAdapter() {
    override fun hyperlinkActivated(event: HyperlinkEvent) = connector.doLogout(authorizationPlace = AuthorizationPlace.SETTINGS)
  }

  private fun subscribeLogInListener() {
    connector.subscribe(object : EduLogInListener {
      override fun userLoggedIn() {
        runInEdt(ModalityState.any()) {
          lastSavedAccount = getCurrentAccount()
          updateLoginLabels()
        }
      }

      override fun userLoggedOut() {
        runInEdt(ModalityState.any()) {
          lastSavedAccount = null
          updateLoginLabels()
        }
      }
    }, disposable)
  }
}