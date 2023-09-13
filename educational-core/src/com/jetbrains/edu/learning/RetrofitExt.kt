package com.jetbrains.edu.learning

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ex.ApplicationUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.PlatformUtils
import com.intellij.util.net.HttpConfigurable
import com.intellij.util.net.ssl.CertificateManager
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.CoursesDownloadingException
import com.jetbrains.edu.learning.stepik.StepikNames
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import okhttp3.logging.HttpLoggingInterceptor.Level.BODY
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.net.HttpURLConnection.*
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

private val LOG = Logger.getInstance("com.jetbrains.edu.learning.RetrofitExt")
const val USER_AGENT = "User-Agent"

fun createRetrofitBuilder(baseUrl: String,
                          connectionPool: ConnectionPool,
                          accessToken: String? = null,
                          authHeaderName: String = "Authorization",
                          authHeaderValue: String? = "Bearer",
                          customInterceptor: Interceptor? = null): Retrofit.Builder {
  return Retrofit.Builder()
    .client(createOkHttpClient(baseUrl, connectionPool, accessToken, authHeaderName, authHeaderValue, customInterceptor))
    .baseUrl(baseUrl)
}

private fun createOkHttpClient(baseUrl: String,
                               connectionPool: ConnectionPool,
                               accessToken: String?,
                               authHeaderName: String,
                               authHeaderValue: String?,
                               customInterceptor: Interceptor?): OkHttpClient {
  val dispatcher = Dispatcher()
  dispatcher.maxRequests = 10

  val logger = HttpLoggingInterceptor { LOG.debug(it) }
  logger.level = if (ApplicationManager.getApplication().isInternal) BODY else BASIC

  val builder = OkHttpClient.Builder()
    .connectionPool(connectionPool)
    .readTimeout(60, TimeUnit.SECONDS)
    .connectTimeout(60, TimeUnit.SECONDS)
    .addInterceptor { chain ->
      val builder = chain.request().newBuilder().addHeader(USER_AGENT, eduToolsUserAgent)
      if (accessToken != null) {
        val authHeader = if (authHeaderValue != null) "$authHeaderValue $accessToken" else accessToken
        builder.addHeader(authHeaderName, authHeader)
      }
      val newRequest = builder.build()
      chain.proceed(newRequest)
    }
    .addInterceptor(logger)
    .dispatcher(dispatcher)

  if (customInterceptor != null) builder.addInterceptor(customInterceptor)

  addProxy(baseUrl, builder)

  return builder.build()
}

fun addProxy(baseUrl: String, builder: OkHttpClient.Builder) {
  val proxyConfigurable = HttpConfigurable.getInstance()
  val proxies = proxyConfigurable.onlyBySettingsSelector.select(URI.create(baseUrl))
  val address = proxies.firstOrNull()?.address() as? InetSocketAddress
  if (address != null) {
    builder.proxy(Proxy(Proxy.Type.HTTP, address))
    builder.proxyAuthenticator(proxyAuthenticator)
  }
  val trustManager = CertificateManager.getInstance().trustManager
  val sslContext = CertificateManager.getInstance().sslContext
  builder.sslSocketFactory(sslContext.socketFactory, trustManager)
}

val proxyAuthenticator: Authenticator
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

val eduToolsUserAgent: String
  get() {
    val version = pluginVersion(EduNames.PLUGIN_ID) ?: "unknown"

    return String.format("%s/version(%s)/%s/%s", StepikNames.PLUGIN_NAME, version, System.getProperty("os.name"),
                         PlatformUtils.getPlatformPrefix())
  }

fun <T> Call<T>.executeHandlingExceptions(omitErrors: Boolean = false): Response<T>? {
  return when (val response = executeParsingErrors(omitErrors)) {
    is Ok -> response.value
    is Err -> null
  }
}

fun <T> Call<T>.executeCall(omitErrors: Boolean = false): Result<Response<T>, String> {
  return try {
    val progressIndicator = ProgressManager.getInstance().progressIndicator

    val response = if (progressIndicator != null) {
      ApplicationUtil.runWithCheckCanceled(Callable { execute() }, progressIndicator)
    }
    else {
      execute()
    }

    ProgressManager.checkCanceled()
    Ok(response)
  }
  catch (e: InterruptedIOException) {
    log("Connection to server was interrupted", e.message, omitErrors)
    Err("${EduCoreBundle.message("error.connection.interrupted")}\n\n${e.message}")
  }
  catch (e: CoursesDownloadingException) {
    log("Failed to connect to server", e.message, true)
    throw e
  }
  catch (e: IOException) {
    log("Failed to connect to server", e.message, omitErrors)
    Err("${EduCoreBundle.message("error.failed.to.connect")} \n\n${e.message}")
  }
  catch (e: ProcessCanceledException) {
    // We don't have to log or throw ProcessCanceledException:
    // 'Control-flow exceptions (like ProcessCanceledException) should never be logged: ignore for explicitly started processes or...'
    cancel()
    Err("Process canceled by user")
  }
  catch (e: RuntimeException) {
    log("Failed to connect to server", e.message, omitErrors)
    Err("${EduCoreBundle.message("error.failed.to.connect")}\n\n${e.message}")
  }
}

private fun log(title: String, message: String?, optional: Boolean) {
  val fullText = "$title. $message"
  if (optional) LOG.warn(fullText) else LOG.error(fullText)
}

fun <T> Call<T>.executeParsingErrors(omitErrors: Boolean = false): Result<Response<T>, String> {
  val response = executeCall(omitErrors).onError { return Err(it) }

  val error = response.errorBody()?.string() ?: return Ok(response)
  log(error, "Code ${response.code()}", omitErrors)

  return when (response.code()) {
    HTTP_OK, HTTP_CREATED, HTTP_NO_CONTENT -> Ok(response) // 200, 201, 204
    HTTP_UNAVAILABLE, HTTP_BAD_GATEWAY ->
      Err("${EduCoreBundle.message("error.service.maintenance")}\n\n$error") // 502, 503
    in HTTP_INTERNAL_ERROR..HTTP_VERSION ->
      Err("${EduCoreBundle.message("error.service.down")}\n\n$error") // 500x
    HTTP_FORBIDDEN, HTTP_UNAUTHORIZED -> {
      val errorMessage = processForbiddenErrorMessage(error) ?: EduCoreBundle.message("error.access.denied")
      Err(errorMessage)
    }
    in HTTP_BAD_REQUEST..HTTP_UNSUPPORTED_TYPE ->
      Err(EduCoreBundle.message("error.unexpected.error", error)) // 400x
    else -> {
      LOG.warn("Code ${response.code()} is not handled")
      Err(EduCoreBundle.message("error.unexpected.error", error))
    }
  }
}

fun <T> Response<T>.checkStatusCode(): Response<T>? {
  if (isSuccessful) return this
  LOG.error("Response is returned with ${this.code()} status code")
  return null
}

fun File.toMultipartBody(): MultipartBody.Part {
  val body = asRequestBody("application/octet-stream".toMediaTypeOrNull())
  return MultipartBody.Part.createFormData("file", this.name, body)
}

fun String.toPlainTextRequestBody(): RequestBody = toRequestBody("text/plain".toMediaTypeOrNull())

private fun processForbiddenErrorMessage(jsonText: String): String? {
  return try {
    val factory = JsonFactory()
    val mapper = ObjectMapper(factory)
    val module = SimpleModule()
    mapper.registerModule(module)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    val courseNode = mapper.readTree(jsonText) as ObjectNode
    courseNode.get("message")?.asText()
  }
  catch (e: ClassCastException) {
    null
  } catch (e: JsonParseException) {
    null
  }
}