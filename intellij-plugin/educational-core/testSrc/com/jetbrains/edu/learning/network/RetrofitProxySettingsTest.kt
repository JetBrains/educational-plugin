package com.jetbrains.edu.learning.network

import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.credentialStore.Credentials
import com.intellij.util.net.ProxyConfiguration
import com.intellij.util.net.ProxyCredentialStore
import com.intellij.util.net.ProxySettings
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.MockWebServerHelper
import com.jetbrains.edu.learning.Ok
import okhttp3.ConnectionPool
import okhttp3.mockwebserver.MockResponse
import org.junit.Test
import retrofit2.Call
import retrofit2.Response
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.http.GET
import java.net.HttpURLConnection.HTTP_PROXY_AUTH
import kotlin.test.assertIs

class RetrofitProxySettingsTest : EduTestCase() {

  @Test
  fun `test proxy settings`() {
    // given
    val message = "Hello from proxy!"
    val proxyHelper = MockWebServerHelper(testRootDisposable)

    proxyHelper.addResponseHandler(testRootDisposable) { _, path ->
      MockResponseFactory.fromString("""{"value": "$message"}""")
    }
    val proxyConfiguration = proxyHelper.proxyConfiguration()

    // when
    val response = withProxyConfiguration(proxyConfiguration) {
      val api = createApi()
      api.call().executeCall()
    }

    // then
    assertIs<Ok<Response<Answer>>>(response)
    assertEquals(Answer(message), response.value.body())
  }

  @Test
  fun `test proxy settings with authorization`() {
    // given
    val proxyHelper = MockWebServerHelper(testRootDisposable)
    val user = "user"
    val password = "password"

    proxyHelper.addResponseHandler(testRootDisposable) { request, path ->
      val proxyAuthValue = request.headers["Proxy-Authorization"]
      if (proxyAuthValue == null) {
        MockResponse().setResponseCode(HTTP_PROXY_AUTH)
      }
      else {
        MockResponseFactory.fromString("""{"value": "$proxyAuthValue"}""")
      }
    }
    val proxyConfiguration = proxyHelper.proxyConfiguration()

    // when
    val response = withProxyConfiguration(proxyConfiguration) {
      withKnownProxyCredentials(proxyConfiguration.host, proxyConfiguration.port, Credentials(user, password)) {
        val api = createApi()
        api.call().executeCall()
      }
    }

    // then
    assertIs<Ok<Response<Answer>>>(response)
    assertEquals(Answer(okhttp3.Credentials.basic(user, password)), response.value.body())
  }

  private fun createApi(): TestApi = createRetrofitBuilder("http://example.com", ConnectionPool())
    .addConverterFactory(JacksonConverterFactory.create())
    .build()
    .create(TestApi::class.java)

  private fun <T> withProxyConfiguration(proxyConf: ProxyConfiguration, body: () -> T): T {
    val previous = ProxySettings.getInstance().getProxyConfiguration()
    ProxySettings.getInstance().setProxyConfiguration(proxyConf)
    assertEquals(ProxySettings.getInstance().getProxyConfiguration(), proxyConf)
    return try {
      body()
    }
    finally {
      ProxySettings.getInstance().setProxyConfiguration(previous)
    }
  }

  private fun <T> withKnownProxyCredentials(host: String, port: Int, credentials: Credentials, body: () -> T): T {
    val store = ProxyCredentialStore.getInstance()
    val previousCreds = store.getCredentials(host, port)
    val previousRemembered = store.areCredentialsRemembered(host, port)
    store.setCredentials(host, port, credentials, false)
    assertEquals(store.getCredentials(host, port), credentials)
    return try {
      body()
    }
    finally {
      store.setCredentials(host, port, previousCreds, previousRemembered)
    }
  }
}

private interface TestApi {
  @GET("/call")
  fun call(): Call<Answer>
}

private data class Answer(@JsonProperty("value") val value: String)
