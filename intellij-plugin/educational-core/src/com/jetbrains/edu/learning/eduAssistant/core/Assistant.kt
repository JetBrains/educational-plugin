package com.jetbrains.edu.learning.eduAssistant.core

import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor

interface Assistant {
  suspend fun getHint(taskProcessor: TaskProcessor, state: EduState, userCode: String? = null): AssistantResponse?
}

data class AssistantResponse(
  val textHint: String? = null,
  val codeHint: String? = null,
  val prompts: Map<String, String> = emptyMap(),
  val assistantError: AssistantError? = null
)
