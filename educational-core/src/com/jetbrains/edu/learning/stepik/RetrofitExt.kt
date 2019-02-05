package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.PlatformUtils
import com.intellij.util.net.HttpConfigurable
import com.intellij.util.net.ssl.CertificateManager
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.pluginVersion
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import okhttp3.logging.HttpLoggingInterceptor.Level.BODY
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.util.concurrent.TimeUnit

private val LOG = Logger.getInstance("com.jetbrains.edu.learning.stepik.RetrofitExt")

fun createRetrofitBuilder(baseUrl: String, accessToken: String? = null): Retrofit.Builder {
  return Retrofit.Builder()
    .client(createOkHttpClient(baseUrl, accessToken))
    .baseUrl(baseUrl)
}

private fun createOkHttpClient(baseUrl: String, accessToken: String?): OkHttpClient {
  val dispatcher = Dispatcher()
  dispatcher.maxRequests = 10

  val logger = HttpLoggingInterceptor { LOG.debug(it) }
  logger.level = if (ApplicationManager.getApplication().isInternal) BODY else BASIC

  val builder = OkHttpClient.Builder()
    .readTimeout(60, TimeUnit.SECONDS)
    .connectTimeout(60, TimeUnit.SECONDS)
    .addInterceptor { chain ->
      val builder = chain.request().newBuilder().addHeader("User-Agent", userAgent)
      if (accessToken != null) {
        builder.addHeader("Authorization", "Bearer ${accessToken}")
      }
      val newRequest = builder.build()
      chain.proceed(newRequest)
    }
    .addInterceptor(logger)
    .dispatcher(dispatcher)

  val proxyConfigurable = HttpConfigurable.getInstance()
  val proxies = proxyConfigurable.onlyBySettingsSelector.select(URI.create(baseUrl))
  val address = if (proxies.size > 0) proxies[0].address() as? InetSocketAddress else null
  if (address != null) {
    builder.proxy(Proxy(Proxy.Type.HTTP, address))
  }
  val trustManager = CertificateManager.getInstance().trustManager
  val sslContext = CertificateManager.getInstance().sslContext
  builder.sslSocketFactory(sslContext.socketFactory, trustManager)

  return builder.build()
}

private val userAgent: String get() {
  val version = pluginVersion(EduNames.PLUGIN_ID) ?: "unknown"

  return String.format("%s/version(%s)/%s/%s", StepikNames.PLUGIN_NAME, version, System.getProperty("os.name"),
                       PlatformUtils.getPlatformPrefix())
}

fun <T> Call<T>.executeHandlingExceptions(): Response<T>? {
  try {
    return this.execute()
  }
  catch (e: IOException) {
    LOG.error("Failed to connect to server. ${e.message}")
  }
  catch (e: RuntimeException) {
    LOG.error("Failed to connect to server. ${e.message}")
  }
  return null
}

fun checkForErrors(response: Response<out Any>?, optional: Boolean = false) {
  if (response == null) return
  val errorBody = response.errorBody()
  if (errorBody == null) return
  when {
    optional -> LOG.warn(errorBody.string())
    else -> LOG.error(errorBody.string())
  }
}