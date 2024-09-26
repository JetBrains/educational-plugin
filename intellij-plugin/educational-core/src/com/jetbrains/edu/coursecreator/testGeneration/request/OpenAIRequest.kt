package com.jetbrains.edu.coursecreator.testGeneration.request

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.project.Project
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.HttpRequests.HttpStatusException
import com.jetbrains.edu.coursecreator.testGeneration.processing.TestRequestedAssembler
import org.jetbrains.research.testspark.core.data.ChatMessage
import org.jetbrains.research.testspark.core.generation.llm.network.RequestManager
import org.jetbrains.research.testspark.core.progress.CustomProgressIndicator
import org.jetbrains.research.testspark.core.test.TestsAssembler
import java.net.HttpURLConnection

// TODO will be replaced with the Grazie requestManager
class OpenAIRequestManager(val project: Project) : RequestManager(System.getenv("OPENAI_TOKEN")) {
  private val url = "https://api.openai.com/v1/chat/completions"

  private val httpRequest = HttpRequests.post(url, "application/json").tuner {
    it.setRequestProperty("Authorization", "Bearer $token")
  }


  override fun send(
    prompt: String,
    indicator: CustomProgressIndicator,
    testsAssembler: TestsAssembler
  ): SendResult {
    val llmRequestBody = OpenAIRequestBody("gpt-3.5-turbo", chatHistory)
    var sendResult = SendResult.OK

      try {
        httpRequest.connect {
          it.write(GsonBuilder().create().toJson(llmRequestBody))

          // check response
          when ((it.connection as HttpURLConnection).responseCode) {
            HttpURLConnection.HTTP_OK -> (testsAssembler as TestRequestedAssembler).consume(it)
            HttpURLConnection.HTTP_INTERNAL_ERROR -> {
              sendResult = SendResult.OTHER
            }

            HttpURLConnection.HTTP_BAD_REQUEST -> {
              sendResult = SendResult.PROMPT_TOO_LONG
            }

            HttpURLConnection.HTTP_UNAUTHORIZED -> {
              sendResult = SendResult.OTHER
            }

            else -> {
              sendResult = SendResult.OTHER
            }
          }
        }
      }
      catch (e: HttpStatusException) {
        log.info { "Error in sending request: ${e.message}" }
      }
    return sendResult

  }

  data class OpenAIRequestBody(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
  )

  data class OpenAIChoice(
    val index: Int,
    val delta: Delta,
    @SerializedName("finish_reason")
    val finishedReason: String,
  )

  data class Delta(val role: String?, val content: String)
}
