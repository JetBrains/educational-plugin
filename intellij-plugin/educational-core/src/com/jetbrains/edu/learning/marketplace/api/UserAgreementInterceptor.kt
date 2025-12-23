package com.jetbrains.edu.learning.marketplace.api

import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.agreement.UserAgreementSettings
import com.jetbrains.edu.learning.network.HTTP_UNAVAILABLE_FOR_LEGAL_REASONS
import com.jetbrains.edu.learning.submissions.UserAgreementState
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Checks if a request returns 451 and if it happens,
 * tries to update user agreements state on remote and repeats the request.
 *
 * Should help with cases when user agreement was accepted locally
 * but for some reason the state was not synced with remote (e.g., due to network issues)
 */
class UserAgreementInterceptor : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val response = chain.makeRequest()

    if (response.code == HTTP_UNAVAILABLE_FOR_LEGAL_REASONS) {
      val (pluginAgreement, aiAgreement) = UserAgreementSettings.getInstance().userAgreementProperties.value
      if (pluginAgreement == UserAgreementState.ACCEPTED) {
        LOG.info("""
          <-- ${response.code} ${response.request.method} ${response.request.url} (unavailable for legal reasons) but user agreement is accepted locally.
          Trying to update user agreement on remote
        """.trimIndent())
        @Suppress("RAW_RUN_BLOCKING")
        val result = runBlocking {
          MarketplaceSubmissionsConnector.getInstance().updateUserAgreements(pluginAgreement, aiAgreement)
        }
        if (result is Ok) {
          response.close()
          return chain.makeRequest()
        }
      }
    }

    return response
  }

  private fun Interceptor.Chain.makeRequest(): Response {
    val request = request()
    return proceed(request)
  }

  companion object {
    private val LOG = logger<UserAgreementInterceptor>()
  }
}
