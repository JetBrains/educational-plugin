package com.jetbrains.edu.coursecreator.testGeneration.request

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.intellij.openapi.progress.runBlockingCancellable
import com.jetbrains.educational.ml.core.grazie.message.ChatHistory
import com.jetbrains.educational.ml.core.grazie.message.ChatMessage
import com.jetbrains.educational.ml.test.generation.TestGenerationClient
import org.jetbrains.research.testspark.core.generation.llm.network.RequestManager
import org.jetbrains.research.testspark.core.progress.CustomProgressIndicator
import org.jetbrains.research.testspark.core.test.TestsAssembler


class AIRequestManager : RequestManager("") {

  private val objectMapper = jsonMapper {
    addModule(kotlinModule())
    enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
  }

  override fun send(
    prompt: String,
    indicator: CustomProgressIndicator,
    testsAssembler: TestsAssembler,
  ): SendResult {
    val result = runBlockingCancellable {
      TestGenerationClient().send(getChatHistoryMlRepresentation())
    }.getOrNull() ?: return SendResult.OTHER

    testsAssembler.consume(result)

    return SendResult.OK
  }

  private fun getChatHistoryMlRepresentation(): ChatHistory {
    val listType = objectMapper.typeFactory.constructCollectionType(
      List::class.java,
      ChatMessage::class.java
    )
    val chat = objectMapper.convertValue<List<ChatMessage>>(chatHistory, listType)
    return ChatHistory(chat)
  }

}
