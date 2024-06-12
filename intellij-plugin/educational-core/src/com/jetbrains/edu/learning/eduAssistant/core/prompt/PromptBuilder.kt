package com.jetbrains.edu.learning.eduAssistant.core.prompt

import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor

interface PromptBuilder {

  fun buildTextHintPrompt(
    taskProcessor: TaskProcessor,
    codeHint: String,
    codeStr: String,
    language: String
  ): String

  fun buildTextHintPromptIfNoCodeHintIsGenerated(
    taskProcessor: TaskProcessor,
    codeStr: String,
    language: String
  ): String

  fun buildCodeHintPrompt(
    taskProcessor: TaskProcessor,
    codeStr: String,
    language: String
  ): String

  fun buildCodeHintPromptFromTextHint(
    taskProcessor: TaskProcessor,
    textHint: String,
    codeStr: String,
    language: String
  ): String
}
