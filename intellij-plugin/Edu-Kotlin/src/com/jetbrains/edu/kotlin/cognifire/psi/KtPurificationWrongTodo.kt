package com.jetbrains.edu.kotlin.cognifire.psi

import com.intellij.openapi.project.Project
import com.jetbrains.edu.cognifire.PurificationWrongTodo
import com.jetbrains.edu.cognifire.models.FunctionSignature
import com.jetbrains.edu.kotlin.cognifire.utils.UNIT_RETURN_VALUE
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeResponse

class KtPurificationWrongTodo : PurificationWrongTodo {
  override fun deleteWrongTodo(project: Project, promptToCode: PromptToCodeResponse, functionSignature: FunctionSignature): PromptToCodeResponse {
    if (functionSignature.returnType != UNIT_RETURN_VALUE) return promptToCode
    val lastCodeLine = promptToCode.maxOfOrNull { it.codeLineNumber } ?: return promptToCode
    val todoSpecifyReturnValueRegex = "(?i).*TODO\\(\".*specify the return value.*\"\\).*".toRegex()
    return promptToCode.filterNot { todoSpecifyReturnValueRegex.containsMatchIn(it.generatedCodeLine) && it.codeLineNumber == lastCodeLine }
  }

}
