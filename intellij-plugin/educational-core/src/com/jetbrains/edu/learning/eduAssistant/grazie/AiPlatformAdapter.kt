package com.jetbrains.edu.learning.eduAssistant.grazie

import ai.grazie.api.gateway.client.SuspendableAPIGatewayClient
import ai.grazie.client.common.SuspendableHTTPClient
import ai.grazie.client.ktor.GrazieKtorHTTPClient
import ai.grazie.model.llm.prompt.LLMPromptID
import com.jetbrains.edu.learning.ai.utils.AiAuthBundle.getGrazieTemporaryToken

object AiPlatformAdapter {
  private val client: SuspendableAPIGatewayClient = initClient()

  private fun getAuthType() = System.getenv("GRAZIE_JWT_TOKEN")?.let { GrazieAuthType.User } ?: GrazieAuthType.Service

  private fun getGrazieToken(authType: GrazieAuthType) = when (authType) {
    GrazieAuthType.User -> System.getenv("GRAZIE_JWT_TOKEN")
    GrazieAuthType.Service -> getGrazieTemporaryToken()
  }

  private fun initClient(): SuspendableAPIGatewayClient {
    val authType = getAuthType()
    val jwtToken = getGrazieToken(authType)
    return SuspendableAPIGatewayClient(
      serverUrl = "https://api.app.stgn.grazie.aws.intellij.net", SuspendableHTTPClient.WithV5(
        GrazieKtorHTTPClient.Client.Default, authType.buildAuthData(jwtToken)
      ), authType.grazieType
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
    val currentProfile = generationContextProfile.getProfileById()
    val sb = StringBuilder()
    try {
      client.llm().v5().chat {
        prompt = LLMPromptID("learning-assistant-prompt")
        profile = currentProfile
        temperature = temp
        messages {
          systemPrompt?.let {
            system(it)
          }
          user(userPrompt)
        }
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
