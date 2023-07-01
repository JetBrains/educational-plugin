package com.jetbrains.edu.markdown.taskDescription

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.jetbrains.edu.codeInsight.taskDescription.EduUriSchemaCompletionProvider

class EduMarkdownCompletionContributor : CompletionContributor() {

  init {
    extend(CompletionType.BASIC, EduMarkdownPsiPatterns.inMarkdownLinkDestination, EduUriSchemaCompletionProvider())
    extend(CompletionType.BASIC, EduMarkdownPsiPatterns.toolWindowIdUriPath, EduMarkdownToolWindowIdCompletionContributor())
    extend(CompletionType.BASIC, EduMarkdownPsiPatterns.settingsIdUriPath, EduMarkdownSettingsIdCompletionContributor())
  }
}
