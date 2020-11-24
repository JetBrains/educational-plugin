package com.jetbrains.edu.html.taskDescription

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.jetbrains.edu.codeInsight.taskDescription.EduUriSchemaCompletionProvider

class EduHtmlCompletionContributor : CompletionContributor() {

  init {
    extend(CompletionType.BASIC, EduHtmlPsiPatterns.inHrefAttributeValue, EduUriSchemaCompletionProvider())
  }
}
