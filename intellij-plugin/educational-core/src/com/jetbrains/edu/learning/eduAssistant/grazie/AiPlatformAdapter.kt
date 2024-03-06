package com.jetbrains.edu.learning.eduAssistant.grazie

import ai.grazie.api.gateway.client.SuspendableAPIGatewayClient
import ai.grazie.client.common.SuspendableHTTPClient
import ai.grazie.client.ktor.GrazieKtorHTTPClient
import ai.grazie.model.auth.v5.AuthData
import ai.grazie.model.cloud.AuthType
import com.jetbrains.edu.learning.ai.utils.AiAuthBundle.getGrazieTemporaryToken
import com.jetbrains.edu.learning.ai.utils.GrazieLlmProfileProvider
import com.jetbrains.edu.learning.eduAssistant.core.AssistantError

object AiPlatformAdapter {
  private val client: SuspendableAPIGatewayClient = initClient()

  private fun getGrazieToken(authType: AuthType) = when (authType) {
    AuthType.User -> System.getenv("GRAZIE_JWT_TOKEN")
            ?: throw AiPlatformException(AssistantError.AuthError, Throwable("Grazie user token was not provided"))
    AuthType.Application -> getGrazieTemporaryToken()
    else -> throw AiPlatformException(AssistantError.AuthError, Throwable("Unsupported auth type: $authType"))
  }

  private fun getServerUrl() = "https://api.app.${GrazieLlmProfileProvider.getServerUrlType()}.grazie.aws.intellij.net"

  private fun initClient(): SuspendableAPIGatewayClient {
    val authType = GrazieLlmProfileProvider.getAuthType()
                   ?: throw AiPlatformException(AssistantError.AuthError, Throwable("Incorrect Grazie auth type"))
    val jwtToken = getGrazieToken(authType)
    return SuspendableAPIGatewayClient(
      serverUrl = getServerUrl(),
      httpClient = SuspendableHTTPClient.WithV5(GrazieKtorHTTPClient.Client.Default, AuthData(jwtToken)),
      authType = authType
    )
  }

  /**
   * Chat completion via LLM model.
   * @throws AiPlatformException in case of any exception (initial exception is stored in **cause**)
   */
  suspend fun chat(
    systemPrompt: String? = null,
    userPrompt: String = "",
    temp: Double = 0.0,
    generationContextProfile: GenerationContextProfile
  ): String {
    val sb = StringBuilder()
    try {
      client.llm().v6().chat {
        generationContextProfile.buildChatRequest(this, systemPrompt, userPrompt, temp)
      }.collect {
        sb.append(it.content)
      }
    }
    catch (e: Throwable) {
      throw e.toAiPlatformException()
    }
    return sb.toString()
  }
}
