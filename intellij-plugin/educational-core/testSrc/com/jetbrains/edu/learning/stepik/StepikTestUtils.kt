package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.network.USER_AGENT
import com.jetbrains.edu.learning.network.eduToolsUserAgent
import com.jetbrains.edu.learning.stepik.StepikNames.getTokenUrl
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import org.apache.commons.codec.binary.Base64
import org.apache.http.Consts
import org.apache.http.HttpStatus
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import java.text.SimpleDateFormat
import java.util.*

object StepikTestUtils {

  private val LOG: Logger = Logger.getInstance(StepikTestUtils::class.java)

  fun loginFakeStepikUser() {
    val fakeToken = TokenInfo().apply { accessToken = "faketoken" }
    EduSettings.getInstance().user = StepikUser.createEmptyUser().apply {
      userInfo = StepikUserInfo("Test User")
      userInfo.id = 1
      saveTokens(fakeToken)
    }
  }

  fun logOutFakeStepikUser() {
    EduSettings.getInstance().user = null
  }

  fun login(disposable: Disposable): StepikUser {
    val tokenInfo = getTokens() ?: error("Failed to get auth token")
    val user = StepikUser(tokenInfo)
    val userInfo = StepikConnector.getInstance().getUserInfo(user, tokenInfo.accessToken)
    if (userInfo != null) {
      user.userInfo = userInfo
    }
    EduSettings.getInstance().user = user
    user.saveTokens(tokenInfo)
    Disposer.register(disposable) {
      EduSettings.getInstance().user = null
    }
    println("Logged in as ${user.firstName} ${user.lastName}")
    return user
  }

  private fun getTokens(): TokenInfo? {
    val parameters = ArrayList<NameValuePair>(listOf(BasicNameValuePair("grant_type", "client_credentials")))

    val clientSecret = System.getenv("STEPIK_TEST_CLIENT_SECRET")
    if (clientSecret == null || clientSecret.isEmpty()) {
      LOG.error("Test client secret is not provided")
      return null
    }

    val clientId = System.getenv("STEPIK_TEST_CLIENT_ID")
    if (clientId == null || clientId.isEmpty()) {
      LOG.error("Test client id is not provided")
      return null
    }

    // If we can't get tokens, there might be a problem with basic auth that used on Stepik by default
    // and disabled for us as we use oauth2 in our tests.
    // Check that:
    // 1. Teamcity didn't change ips of agent
    // 2. Stepik still have our server in whitelist
    return getTokens(parameters, "$clientId:$clientSecret")
  }

  private fun getTokens(parameters: List<NameValuePair>, credentials: String): TokenInfo? {
    val request = HttpPost(getTokenUrl())
    request.addHeader("Authorization", "Basic " + Base64.encodeBase64String(credentials.toByteArray(Consts.UTF_8)))
    request.addHeader(USER_AGENT, eduToolsUserAgent)
    request.entity = UrlEncodedFormEntity(parameters, Consts.UTF_8)

    val httpClient = HttpClients.createDefault()
    val response = httpClient.execute(request)
    val statusLine = response.statusLine
    val responseEntity = response.entity
    val responseString = if (responseEntity != null) EntityUtils.toString(responseEntity) else ""
    EntityUtils.consume(responseEntity)
    if (statusLine.statusCode == HttpStatus.SC_OK) {
      return StepikConnector.getInstance().objectMapper.readValue(responseString, TokenInfo::class.java)
    }
    else {
      LOG.warn("Failed to get tokens: " + statusLine.statusCode + statusLine.reasonPhrase)
    }

    return null
  }

  fun Date.format(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
    return formatter.format(this)
  }
}
