package com.jetbrains.edu.markdown.taskDescription

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.jetbrains.edu.coursecreator.taskDescription.EduUriSchemaCompletionProvider

class EduMarkdownCompletionContributor : CompletionContributor() {

  init {
    extend(CompletionType.BASIC, EduMarkdownPsiPatterns.inMarkdownLinkDestination, EduUriSchemaCompletionProvider())
  }
}
