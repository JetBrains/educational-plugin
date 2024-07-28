package com.jetbrains.edu.learning.network

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.logger
import com.jetbrains.edu.learning.courseFormat.message
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
import java.net.HttpURLConnection.*
import java.util.concurrent.TimeUnit


private val LOG = logger("com.jetbrains.edu.learning.RetrofitExt")
const val USER_AGENT = "User-Agent"

fun createRetrofitBuilder(
  baseUrl: String,
  connectionPool: ConnectionPool,
  accessToken: String? = null,
  authHeaderName: String = "Authorization",
  authHeaderValue: String? = "Bearer",
  customInterceptor: Interceptor? = null
): Retrofit.Builder {
  return Retrofit.Builder()
    .client(createOkHttpClient(baseUrl, connectionPool, accessToken, authHeaderName, authHeaderValue, customInterceptor))
    .baseUrl(baseUrl)
}

private fun createOkHttpClient(
  baseUrl: String,
  connectionPool: ConnectionPool,
  accessToken: String?,
  authHeaderName: String,
  authHeaderValue: String?,
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

  if (customInterceptor != null) {
    builder.addInterceptor(customInterceptor)
  }

  builder.customizeClient(baseUrl)

  return builder.build()
}

fun OkHttpClient.Builder.customizeClient(baseUrl: String) : OkHttpClient.Builder {
  val executor = findService(RetrofitHelper::class.java)
  return executor.customizeClient(this, baseUrl)
}

val eduToolsUserAgent: String
  get() {
    return findService(RetrofitHelper::class.java).eduToolsUserAgent
  }

fun <T> Call<T>.executeHandlingExceptions(omitErrors: Boolean = false): Response<T>? {
  return when (val response = executeParsingErrors(omitErrors)) {
    is Ok -> response.value
    is Err -> null
  }
}

fun <T> Call<T>.executeCall(omitErrors: Boolean = false): Result<Response<T>, String> {
  val executor = findService(RetrofitHelper::class.java)
  return executor.executeCall(this, omitErrors)
}

fun <T> Call<T>.executeParsingErrors(omitErrors: Boolean = false): Result<Response<T>, String> {
  val response = executeCall(omitErrors).onError { return Err(it) }
  return response.executeParsingErrors(omitErrors)
}

fun <T> Response<T>.executeParsingErrors(omitErrors: Boolean = false): Result<Response<T>, String> {
  val error = errorBody()?.string() ?: return Ok(this)
  val code = code()
  val fullErrorText = "$error. Code $code"
  if (omitErrors) LOG.warning(fullErrorText) else LOG.severe(fullErrorText)

  return when (code) {
    HTTP_OK, HTTP_CREATED, HTTP_ACCEPTED, HTTP_NO_CONTENT -> Ok(this) // 200, 201, 202, 204
    HTTP_UNAVAILABLE, HTTP_BAD_GATEWAY ->
      Err("${message("error.service.maintenance")}\n\n$error") // 502, 503
    in HTTP_INTERNAL_ERROR..HTTP_VERSION ->
      Err("${message("error.service.down")}\n\n$error") // 500x
    HTTP_FORBIDDEN, HTTP_UNAUTHORIZED -> {
      val errorMessage = processForbiddenErrorMessage(error) ?: message("error.access.denied")
      Err(errorMessage)
    }
    HTTP_UNAVAILABLE_FOR_LEGAL_REASONS -> { // 451
      LOG.warning(message("error.agreement.not.accepted"))
      Err(fullErrorText)
    }
    in HTTP_BAD_REQUEST..HTTP_UNSUPPORTED_TYPE ->
      Err(message("error.unexpected.error", error)) // 400x
    else -> {
      LOG.warning("Code $code is not handled")
      Err(message("error.unexpected.error", error))
    }
  }
}

fun <T> Response<T>.checkStatusCode(): Response<T>? {
  if (isSuccessful) return this
  LOG.severe("Response is returned with ${this.code()} status code")
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

const val HTTP_UNAVAILABLE_FOR_LEGAL_REASONS: Int = 451
