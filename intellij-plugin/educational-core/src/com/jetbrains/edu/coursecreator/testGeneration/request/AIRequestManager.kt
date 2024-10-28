package com.jetbrains.edu.coursecreator.testGeneration.request

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.progress.runBlockingCancellable
import com.jetbrains.educational.ml.test.generation.TestGenerationClient
import org.jetbrains.research.testspark.core.generation.llm.network.RequestManager
import org.jetbrains.research.testspark.core.progress.CustomProgressIndicator
import org.jetbrains.research.testspark.core.test.TestsAssembler


class AIRequestManager : RequestManager("") {

  override fun send(
    prompt: String,
    indicator: CustomProgressIndicator,
    testsAssembler: TestsAssembler,
  ): SendResult {
    val objectMapper = ObjectMapper()
    val chatHistoryJson = objectMapper.writeValueAsString(chatHistory)

    val result = runBlockingCancellable {
      TestGenerationClient().send(chatHistoryJson)
    }.getOrNull() ?: return SendResult.OTHER

    testsAssembler.consume(result)

    return SendResult.OK
  }
}
