package com.jetbrains.edu.learning.network

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.application.ex.ApplicationUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.PlatformUtils
import com.intellij.util.net.*
import com.intellij.util.net.ssl.CertificateManager
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.StepikNames
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
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
import java.util.concurrent.TimeUnit


private val LOG = Logger.getInstance("com.jetbrains.edu.learning.RetrofitExt")
const val USER_AGENT = "User-Agent"

fun createRetrofitBuilder(
  baseUrl: String,
  connectionPool: ConnectionPool,
  accessToken: String? = null,
  authHeaderName: String = "Authorization",
  authHeaderValue: String? = "Bearer",
  retryPolicy: RetryPolicy? = RetryPolicy(),
  customInterceptor: Interceptor? = null
): Retrofit.Builder {
  return Retrofit.Builder()
    .client(createOkHttpClient(baseUrl, connectionPool, accessToken, authHeaderName, authHeaderValue, retryPolicy, customInterceptor))
    .addCallAdapterFactory(NetworkResultCallAdapterFactory)
    .baseUrl(baseUrl)
}

private fun createOkHttpClient(
  baseUrl: String,
  connectionPool: ConnectionPool,
  accessToken: String?,
  authHeaderName: String,
  authHeaderValue: String?,
  retryPolicy: RetryPolicy?,
  customInterceptor: Interceptor?
): OkHttpClient {
  val dispatcher = Dispatcher()
  dispatcher.maxRequests = 10

  val logger = HttpLoggingInterceptor { LOG.info(it) }
  logger.level = BASIC

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

  if (retryPolicy != null) {
    builder.addInterceptor(RetryInterceptor(retryPolicy))
  }

  if (customInterceptor != null) {
    builder.addInterceptor(customInterceptor)
  }

  builder.customizeClient(baseUrl)

  return builder.build()
}

fun OkHttpClient.Builder.customizeClient(baseUrl: String) : OkHttpClient.Builder {
  return addProxy(baseUrl)
    .addEdtAssertions()
    .addHostAssertions()
}

private val proxyAuthenticator: Authenticator
  get() = Authenticator { _, response ->
    val provider = ProxyCredentialStore.getInstance().asProxyCredentialProvider()
    val ideProxyCredentials = ProxySettings.getInstance().getStaticProxyCredentials(provider) ?: return@Authenticator null
    val login = ideProxyCredentials.userName ?: return@Authenticator null
    val password = ideProxyCredentials.getPasswordAsString() ?: return@Authenticator null

    val credentials = Credentials.basic(login, password)
    response.request.newBuilder()
      .header("Proxy-Authorization", credentials)
      .build()
  }

private fun OkHttpClient.Builder.addProxy(baseUrl: String): OkHttpClient.Builder {
  val proxies = JdkProxyCustomizer.getInstance().originalProxySelector.select(URI.create(baseUrl))
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
    NetworkRequestEDTAssertionPolicy.assertIsDispatchThread()
    val request = chain.request()
    chain.proceed(request)
  }
}

private fun OkHttpClient.Builder.addHostAssertions(): OkHttpClient.Builder {
  if (!isUnitTestMode) return this

  return addInterceptor { chain ->
    val request = chain.request()
    val host = request.url.host

    TestNetworkRequestManager.getInstance().checkHostIsAllowed(host)

    chain.proceed(request)
  }
}

val eduToolsUserAgent: String
  get() {
    val version = pluginVersion(EduNames.PLUGIN_ID) ?: "unknown"
    return "${StepikNames.PLUGIN_NAME}/version($version)/${System.getProperty("os.name")}/${PlatformUtils.getPlatformPrefix()}"
  }

fun <T> Call<T>.executeHandlingExceptions(omitErrors: Boolean = false): Response<T>? {
  return when (val response = executeParsingErrors(omitErrors)) {
    is Ok -> response.value
    is Err -> null
  }
}

private fun log(message: String, omitErrors: Boolean) {
  if (omitErrors) {
    LOG.warn(message)
  }
  else {
    LOG.error(message)
  }
}

fun <T> Call<T>.executeCall(omitErrors: Boolean = false): Result<Response<T>, String> {
  val networkResult = executeWithCheckCanceled()

  return networkResult.mapErr { error ->
    log("${error.title}. ${error.exceptionMessage}", omitErrors)
    error.message
  }
}

fun <T> Call<T>.executeParsingErrors(omitErrors: Boolean = false): Result<Response<T>, String> {
  val response = executeCall(omitErrors).onError { return Err(it) }
  return response.executeParsingErrors(omitErrors)
}

private fun <T> Response<T>.executeParsingErrors(omitErrors: Boolean = false): Result<Response<T>, String> {
  return transformWithErrorCodeMapping { this }
    .mapErr { (code, error) ->
      log("$error. Code $code", omitErrors)
      error
    }
}

fun <T> Response<T>.checkStatusCode(): Response<T>? {
  if (isSuccessful) return this
  LOG.error("Response is returned with ${this.code()} status code")
  return null
}

fun File.toMultipartBody(name: String = "file"): MultipartBody.Part {
  val body = asRequestBody("application/octet-stream".toMediaTypeOrNull())
  return MultipartBody.Part.createFormData(name, this.name, body)
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

const val HTTP_UNAVAILABLE_FOR_LEGAL_REASONS: Int = 451

/**
 * Executes call checking for cancellation if it's launched under progress
 */
fun <T> Call<T>.executeWithCheckCanceled(): Result<Response<T>, NetworkError.Exception> {
  return try {
    @Suppress("UsagesOfObsoleteApi")
    val progressIndicator = ProgressManager.getInstance().progressIndicator

    val response = if (progressIndicator != null) {
      ApplicationUtil.runWithCheckCanceled({ execute() }, progressIndicator)
    }
    else {
      execute()
    }

    ProgressManager.checkCanceled()
    Ok(response)
  }
  catch (e: Exception) {
    if (e is ProcessCanceledException) {
      cancel()
    }
    val networkError = e.toNetworkError() ?: throw e
    Err(networkError)
  }
}

fun Exception.toNetworkError(): NetworkError.Exception? {
  return when (this) {
    is InterruptedIOException -> NetworkError.Exception(EduCoreBundle.message("error.network.connection.interrupted"), this)
    is IOException -> NetworkError.Exception(EduCoreBundle.message("error.network.failed.to.connect"), this)
    is ProcessCanceledException -> NetworkError.Exception("Process canceled by user")
    is RuntimeException -> NetworkError.Exception(EduCoreBundle.message("error.network.failed.to.connect"), this)
    else -> null
  }
}

fun <T, R> Response<T>.transformWithErrorCodeMapping(transform: Response<T>.() -> R): Result<R, NetworkError.HttpError> {
  return if (isSuccessful) {
    Ok(transform())
  }
  else {
    val code = code()
    val error = errorBody()?.string().orEmpty()

    val message = when (code) {
      HTTP_UNAVAILABLE, HTTP_BAD_GATEWAY -> {
        "${EduCoreBundle.message("error.network.service.maintenance")}\n\n$error"// 502, 503
      }
      in HTTP_INTERNAL_ERROR..HTTP_VERSION -> {
        "${EduCoreBundle.message("error.network.service.down")}\n\n$error" // 500x
      }
      HTTP_FORBIDDEN, HTTP_UNAUTHORIZED -> {
        processForbiddenErrorMessage(error) ?: EduCoreBundle.message("error.network.access.denied")
      }
      HTTP_UNAVAILABLE_FOR_LEGAL_REASONS -> { // 451
        "${EduCoreBundle.message("error.network.agreement.not.accepted")}\n\n$error"
      }
      else -> {
        EduCoreBundle.message("error.network.unexpected.error", error)
      }
    }

    Err(NetworkError.HttpError(code, message))
  }
}
