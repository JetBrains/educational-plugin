package com.jetbrains.edu.learning.network

import com.intellij.openapi.application.ex.ApplicationUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.PlatformUtils
import com.intellij.util.net.HttpConfigurable
import com.intellij.util.net.ssl.CertificateManager
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.messages.EduFormatBundle
import com.jetbrains.edu.learning.newproject.CoursesDownloadingException
import com.jetbrains.edu.learning.stepik.StepikNames
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI

/**
 * This is a service class, NOT intended to be instantiated directly
 *
 * @see com.jetbrains.edu.learning.network.RetrofitHelper
 * @see com.jetbrains.edu.learning.findService
 */
class RetrofitHelperImpl : RetrofitHelper {
  override fun <T> executeCall(call: Call<T>, omitErrors: Boolean): Result<Response<T>, String> {
    return try {
      val progressIndicator = ProgressManager.getInstance().progressIndicator

      val response = if (progressIndicator != null) {
        ApplicationUtil.runWithCheckCanceled({ call.execute() }, progressIndicator)
      }
      else {
        call.execute()
      }

      ProgressManager.checkCanceled()
      Ok(response)
    }
    catch (e: InterruptedIOException) {
      log("Connection to server was interrupted", e.message, omitErrors)
      Err("${EduFormatBundle.message("error.connection.interrupted")}\n\n${e.message}")
    }
    catch (e: CoursesDownloadingException) {
      log("Failed to connect to server", e.message, true)
      throw e
    }
    catch (e: IOException) {
      log("Failed to connect to server", e.message, omitErrors)
      Err("${EduFormatBundle.message("error.failed.to.connect")} \n\n${e.message}")
    }
    catch (e: ProcessCanceledException) {
      // We don't have to LOG.log or throw ProcessCanceledException:
      // 'Control-flow exceptions (like ProcessCanceledException) should never be LOG.logged: ignore for explicitly started processes or...'
      call.cancel()
      Err("Process canceled by user")
    }
    catch (e: RuntimeException) {
      log("Failed to connect to server", e.message, omitErrors)
      Err("${EduFormatBundle.message("error.failed.to.connect")}\n\n${e.message}")
    }
  }

  override fun customizeClient(builder: OkHttpClient.Builder, baseUrl: String): OkHttpClient.Builder  {
    return builder
      .addProxy(baseUrl)
      .addEdtAssertions()
  }

  private fun OkHttpClient.Builder.addProxy(baseUrl: String): OkHttpClient.Builder {
    val proxyConfigurable = HttpConfigurable.getInstance()
    val proxies = proxyConfigurable.onlyBySettingsSelector.select(URI.create(baseUrl))
    val address = proxies.firstOrNull()?.address() as? InetSocketAddress
    if (address != null) {
      proxy(Proxy(Proxy.Type.HTTP, address))
      proxyAuthenticator(proxyAuthenticator)
    }
    val trustManager = CertificateManager.getInstance().trustManager
    val sslContext = CertificateManager.getInstance().sslContext
    return sslSocketFactory(sslContext.socketFactory, trustManager)
  }

  private fun OkHttpClient.Builder.addEdtAssertions(): OkHttpClient.Builder {
    return addInterceptor { chain ->
      NetworkRequestAssertionPolicy.assertIsDispatchThread()
      val request = chain.request()
      chain.proceed(request)
    }
  }

  @Suppress("UnstableApiUsage")
  override val eduToolsUserAgent: String
    get() {
      val version = pluginVersion(EduNames.PLUGIN_ID) ?: "unknown"

      return String.format(
        "%s/version(%s)/%s/%s", StepikNames.PLUGIN_NAME, version, System.getProperty("os.name"), PlatformUtils.getPlatformPrefix()
      )
    }

  private val proxyAuthenticator: Authenticator
    get() = Authenticator { _, response ->
      val proxyConfigurable = HttpConfigurable.getInstance()
      if (proxyConfigurable.PROXY_AUTHENTICATION && proxyConfigurable.proxyLogin != null) {
        val login = proxyConfigurable.proxyLogin ?: return@Authenticator null
        val password = proxyConfigurable.plainProxyPassword ?: return@Authenticator null

        val credentials = Credentials.basic(login, password)
        return@Authenticator response.request.newBuilder()
          .header("Proxy-Authorization", credentials)
          .build()
      }

      null
    }

  private fun log(title: String, message: String?, optional: Boolean) {
    val fullText = "$title. $message"
    if (optional) LOG.warn(fullText) else LOG.error(fullText)
  }

  companion object {
    private val LOG = logger<RetrofitHelperImpl>()
  }
}
