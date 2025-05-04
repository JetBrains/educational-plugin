package com.jetbrains.edu.html.taskDescription

import com.intellij.codeInsight.completion.CompletionParameters
import com.jetbrains.edu.codeInsight.taskDescription.EduUriSchemaCompletionProviderBase

class EduHtmlUriSchemaCompletionProvider : EduUriSchemaCompletionProviderBase() {
  override fun linkPrefix(parameters: CompletionParameters): String {
    val position = parameters.position
    return position.text.substring(0, parameters.offset - position.textRange.startOffset)
  }
}
