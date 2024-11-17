package com.jetbrains.edu.aiHints.core.connector

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.ui.JBAccountInfoService
import com.jetbrains.edu.ai.host.EduAIServiceHost
import com.jetbrains.edu.aiHints.core.service.HintsService
import com.jetbrains.edu.learning.network.createRetrofitBuilder
import com.jetbrains.educational.ml.hints.context.CodeHintContext
import com.jetbrains.educational.ml.hints.context.TextHintContext
import com.jetbrains.educational.ml.hints.hint.CodeHint
import com.jetbrains.educational.ml.hints.hint.TextHint
import okhttp3.ConnectionPool

@Service(Service.Level.APP)
class HintsServiceConnector {
  private val url: String
    get() = EduAIServiceHost.getSelectedUrl()

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

  suspend fun getCodeHint(context: CodeHintContext): CodeHint {
    val response = service.getCodeHint(context).body() ?: error("Response body is null")
    return response.asCodeHint()
  }

  suspend fun getTextHint(context: TextHintContext): TextHint {
    val response = service.getTextHint(context).body() ?: error("Response body is null")
    return response.asTextHint()
  }

  companion object {
    fun getInstance(): HintsServiceConnector = service()

    fun List<CodeHint>.asCodeHint() = CodeHint(joinToString(separator = "") { it.code })

    fun List<TextHint>.asTextHint() = TextHint(joinToString(separator = "") { it.text })

    private val LOG: Logger = thisLogger()
  }
}