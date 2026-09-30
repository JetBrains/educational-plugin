package com.jetbrains.edu.learning.marketplace.api

import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.ui.JBAccountInfoService
import com.intellij.ui.JBAccountInfoService.AccessTokenResult
import com.intellij.ui.JBAccountInfoService.AuthRequired
import java.util.concurrent.Future


// BACKCOMPAT: 2026.1. Inline
internal fun JBAccountInfoService.getJBAuthAccessToken(): Future<String?> {
  return getGlobalAccessToken(MarketplaceAuthConnector.JB_AUTHN_SERVICE_AUDIENCE)
    .thenApply { result ->
      val accessToken = when (result) {
        AuthRequired.INSTANCE -> null
        is AccessTokenResult.AccessToken -> result.accessToken
        is AccessTokenResult.RequestFailed -> {
          fileLogger().warn("""JB Auth token request failed with ${result.httpStatusCode} code and "${result.message}" message""")
          null
        }
      }
      accessToken
    }
}
