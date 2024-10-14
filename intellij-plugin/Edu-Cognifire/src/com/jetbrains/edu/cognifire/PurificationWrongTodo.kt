package com.jetbrains.edu.cognifire

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.ThreadingAssertions
import com.jetbrains.edu.cognifire.models.FunctionSignature
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeResponse

interface PurificationWrongTodo {
  fun deleteWrongTodo(project: Project, promptToCode: PromptToCodeResponse, functionSignature: FunctionSignature): PromptToCodeResponse

  companion object {
    private val EP_NAME = LanguageExtension<PurificationWrongTodo>("Educational.purificationWrongTodo")

    fun deleteWrongTodo(project: Project, promptToCode: PromptToCodeResponse, functionSignature: FunctionSignature, language: Language): PromptToCodeResponse {
      ThreadingAssertions.assertReadAccess()
      return EP_NAME.forLanguage(language).deleteWrongTodo(project, promptToCode, functionSignature)
    }
  }
}