package com.jetbrains.edu.learning.api

import com.intellij.ide.BrowserUtil
import com.intellij.util.Urls
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.authUtils.CustomAuthorizationServer
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.authUtils.OAuthUtils
import com.jetbrains.edu.learning.courseFormat.UserInfo
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import org.jetbrains.ide.BuiltInServerManager
import java.io.IOException

/**
 * Base class for OAuthConnectors using [Authorization Code Flow](https://auth0.com/docs/get-started/authentication-and-authorization-flow/authorization-code-flow)
 */
abstract class EduOAuthCodeFlowConnector<Account : OAuthAccount<*>, SpecificUserInfo : UserInfo>: EduLoginConnector<Account, SpecificUserInfo>() {
  protected abstract val authorizationUrl: String

  @Synchronized
  override fun doAuthorize(
    vararg postLoginActions: Runnable,
    authorizationPlace: EduCounterUsageCollector.AuthorizationPlace
  ) {
    if (!OAuthUtils.checkBuiltinPortValid()) return

    this.authorizationPlace = authorizationPlace
    setPostLoginActions(postLoginActions.asList())
    BrowserUtil.browse(authorizationUrl)
  }

  /**
   * Must be synchronized to avoid race condition
   */
  abstract fun login(code: String): Boolean

  @Throws(IOException::class)
  private fun createCustomServer(): CustomAuthorizationServer {
    return CustomAuthorizationServer.create(platformName, oAuthServicePath) { code: String, _: String ->
      if (!login(code)) "Failed to log in to $platformName" else null
    }
  }

  protected open fun getRedirectUri(): String =
    if (EduUtils.isAndroidStudio()) {
      val runningServer = CustomAuthorizationServer.getServerIfStarted(platformName)
      val server = runningServer ?: createCustomServer()
      server.handlingUri
    }
    else {
      // port is already checked to be valid
      val currentPort = BuiltInServerManager.getInstance().port
      Urls.newHttpUrl("$redirectHost:${currentPort}", oAuthServicePath).toString()
    }

}