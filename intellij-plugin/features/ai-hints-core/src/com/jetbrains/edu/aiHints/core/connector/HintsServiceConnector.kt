package com.jetbrains.edu.aiHints.core.connector

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.JBAccountInfoService
import com.jetbrains.edu.ai.host.EduAIServiceHost
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.aiHints.core.service.HintsService
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.network.HTTP_UNAVAILABLE_FOR_LEGAL_REASONS
import com.jetbrains.edu.learning.network.createRetrofitBuilder
import com.jetbrains.edu.learning.onError
import com.jetbrains.educational.ml.hints.context.CodeHintContext
import com.jetbrains.educational.ml.hints.context.TextHintContext
import com.jetbrains.educational.ml.hints.hint.CodeHint
import com.jetbrains.educational.ml.hints.hint.TextHint
import okhttp3.ConnectionPool
import retrofit2.Response
import java.net.HttpURLConnection.*

@Service(Service.Level.APP)
class HintsServiceConnector {
  private val url: String
    get() = EduAIServiceHost.selectedHost.url

  private val connectionPool = ConnectionPool()

  private val service: HintsService
    get() = createHintsService()

  @Throws(IllegalStateException::class)
  private fun createHintsService(): HintsService {
    val userId = JBAccountInfoService.getInstance()?.userData?.id ?: run {
      LOG.error("JetBrains Account User ID is null")
      throw IllegalStateException("JetBrains Account User ID is null")
    }
    return createRetrofitBuilder(url, connectionPool, "u.$userId")
      .addConverterFactory(HintsConverterFactory())
      .build()
      .create(HintsService::class.java)
  }

  /**
   * We throw the [Err]or with bundled string further because we catch them on the `educational-ml-library` side.
   * This is a bad design, and we must reimplement this ([EDU-7696](https://youtrack.jetbrains.com/issue/EDU-7696)).
   */
  suspend fun getCodeHint(context: CodeHintContext): CodeHint {
    val response = service.getCodeHint(context).handleResponse().onError {
      error(it)
    }
    return response.asCodeHint()
  }

  /**
   * We throw the [Err]or with bundled string further because we catch them on the `educational-ml-library` side.
   * This is a bad design, and we must reimplement this ([EDU-7696](https://youtrack.jetbrains.com/issue/EDU-7696)).
   */
  suspend fun getTextHint(context: TextHintContext): TextHint {
    val response = service.getTextHint(context).handleResponse().onError {
      error(it)
    }
    return response.asTextHint()
  }

  /**
   * We put strings from bundle to the [Err]ors, because we then show them to user.
   *
   * @see [com.jetbrains.edu.aiHints.core.HintsLoader.getHint]
   */
  private fun <T> Response<List<T>>.handleResponse(): Result<List<T>, String> {
    val code = code()
    if (!isSuccessful) {
      LOG.warn("Request failed. Status code: $code. Error message: ${errorBody()?.string().toString()}")
    }
    val body = body()
    return when {
      code == HTTP_OK && body != null -> Ok(body)
      code == HTTP_UNAVAILABLE_FOR_LEGAL_REASONS -> Err(EduAIHintsCoreBundle.message("hints.error.unavailable.for.legal.reasons"))
      code == HTTP_UNAVAILABLE || code == HTTP_BAD_GATEWAY -> Err(EduAIHintsCoreBundle.message("hints.error.service.unavailable"))
      body == null -> Err(EduAIHintsCoreBundle.message("hints.error.failed.to.generate"))
      else -> Err(EduAIHintsCoreBundle.message("hints.error.unknown"))
    }
  }

  companion object {
    fun getInstance(): HintsServiceConnector = service()

    fun List<CodeHint>.asCodeHint() = CodeHint(joinToString(separator = "") { it.code })

    fun List<TextHint>.asTextHint() = TextHint(joinToString(separator = "") { it.text })

    private val LOG: Logger = logger<HintsServiceConnector>()
  }
}