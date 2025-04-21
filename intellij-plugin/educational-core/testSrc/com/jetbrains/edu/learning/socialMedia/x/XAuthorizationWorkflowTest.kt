package com.jetbrains.edu.learning.socialMedia.x

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.replaceService
import com.intellij.util.Url
import com.intellij.util.Urls
import com.intellij.util.application
import com.intellij.util.io.HttpRequests
import com.intellij.util.queryParameters
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.rules.WithRegistryValue
import io.mockk.every
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.ide.BuiltInServerManager
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertNotEquals

@WithRegistryValue("edu.socialMedia.x.oauth2", "true")
class XAuthorizationWorkflowTest : EduTestCase() {

  private lateinit var helper: MockWebServerHelper
  private lateinit var eduBrowser: EduBrowser

  private val responseCode = AtomicInteger(NO_RESPONSE)

  override fun setUp() {
    super.setUp()

    inMemoryPasswordSafe(testRootDisposable)
    helper = MockWebServerHelper(testRootDisposable)
    application.replaceService(XConnector::class.java, XConnector(helper.baseUrl, helper.baseUrl, "12345"), testRootDisposable)
    eduBrowser = mockService<EduBrowser>(application)

    BuiltInServerManager.getInstance().waitForStart()
  }

  @Test
  fun `test successful authorization`() {
    // given
    configureAuthResponse { parsedUri ->
      addParameters(mapOf("state" to parsedUri.state, "code" to CODE_VALUE))
    }

    helper.addResponseHandler(testRootDisposable) { request, path ->
      when (path) {
        "/2/oauth2/token" -> {
          val rawParams = request.body.readUtf8()
          val params = URI.create("http://localhost?$rawParams").queryParameters

          // Verify that we properly pass code value passed to redirect_uri
          if (params["code"] != CODE_VALUE) return@addResponseHandler MockResponseFactory.badRequest()

          //language=json
          MockResponseFactory.fromString("""{
            "token_type" : "bearer",
            "expires_in" : 7200,
            "access_token" : "$ACCESS_TOKEN",
            "refresh_token" : "$REFRESH_TOKEN",
            "scope" : "offline.access tweet.write media.write users.read tweet.read"
          }""".trimIndent())
        }
        "/2/users/me" -> {
          // Verify that we pass the proper access token here
          if (request.getHeader("Authorization") != "Bearer $ACCESS_TOKEN") {
            return@addResponseHandler MockResponseFactory.badRequest()
          }
          //language=json
          MockResponseFactory.fromString("""{
            "data": {
              "id": "2244994945",
              "name": "X Dev",
              "username": "TwitterDev"
            }
          }""".trimIndent())
        }
        else -> MockResponseFactory.notFound()
      }
    }

    // when
    XConnector.getInstance().doAuthorize()
    PlatformTestUtil.waitWhileBusy { responseCode.get() == NO_RESPONSE }

    // then
    assertEquals(HttpURLConnection.HTTP_OK, responseCode.get())

    val account = kotlin.test.assertNotNull(XSettings.getInstance().account)
    assertNotEquals(-1, account.tokenExpiresIn)
    assertEquals("X Dev", account.userInfo.name)
    assertEquals("TwitterDev", account.userInfo.userName)
    assertEquals("12345", account.getAccessToken())
    assertEquals("67890", account.getRefreshToken())
  }

  @Test
  fun `test error response`() {
    // given
    configureAuthResponse { parsedUri ->
      addParameters(mapOf("error" to "access_denied", "state" to parsedUri.state))
    }
    // when
    XConnector.getInstance().doAuthorize()
    PlatformTestUtil.waitWhileBusy { responseCode.get() == NO_RESPONSE }

    // then
    assertEquals(HttpURLConnection.HTTP_OK, responseCode.get())

    assertNull(XSettings.getInstance().account)
  }

  @Test
  fun `test unrelated response`() {
    // given
    configureAuthResponse { parsedUri ->
      addParameters(mapOf("foo" to "bar"))
    }
    // when
    XConnector.getInstance().doAuthorize()
    PlatformTestUtil.waitWhileBusy { responseCode.get() == NO_RESPONSE }

    // then
    assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, responseCode.get())

    assertNull(XSettings.getInstance().account)
  }

  @Test
  fun `test X Api does not respond`() {
    // given
    configureAuthResponse { parsedUri ->
      addParameters(mapOf("state" to parsedUri.state, "code" to CODE_VALUE))
    }
    helper.addResponseHandler(testRootDisposable) { _, _ -> MockResponseFactory.notFound() }

    // when
    XConnector.getInstance().doAuthorize()
    PlatformTestUtil.waitWhileBusy { responseCode.get() == NO_RESPONSE }

    // then
    assertEquals(HttpURLConnection.HTTP_OK, responseCode.get())

    assertNull(XSettings.getInstance().account)
  }

  private fun configureAuthResponse(adjustUrl: Url.(ParsedAuthUri) -> Url) {
    every { eduBrowser.browse(any<String>()) } answers {
      val parsedUri = parseAuthUri(firstArg<String>())

      makeAuthResponse(parsedUri, adjustUrl) { code ->
        responseCode.set(code)
      }
    }
  }

  private fun makeAuthResponse(parsedUri: ParsedAuthUri, adjustUrl: Url.(ParsedAuthUri) -> Url, onFinish: (Int) -> Unit) {
    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch {
      val url = Urls.parse(parsedUri.redirectUri, false)!!
        .adjustUrl(parsedUri)

      val code = runCatching {
        HttpRequests.request(url).throwStatusCodeException(false).tryConnect()
      }.getOrElse { -1 }
      onFinish(code)
    }

    Disposer.register(testRootDisposable) { job.cancel() }
  }

  private fun parseAuthUri(rawUrl: String): ParsedAuthUri {
    val uri = URI.create(rawUrl)

    val params = uri.queryParameters

    val redirectUri = params.getValue("redirect_uri")
    val state = params.getValue("state")

    return ParsedAuthUri(redirectUri = redirectUri, state = state)
  }

  companion object {
    private const val NO_RESPONSE = 0

    private const val ACCESS_TOKEN = "12345"
    private const val REFRESH_TOKEN = "67890"

    private const val CODE_VALUE = "code"
  }

  private data class ParsedAuthUri(
    val redirectUri: String,
    val state: String
  )
}
