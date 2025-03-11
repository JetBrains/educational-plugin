package com.jetbrains.edu.markdown.taskDescription

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType

class EduMarkdownCompletionContributor : CompletionContributor() {

  init {
    extend(CompletionType.BASIC, EduMarkdownPsiPatterns.inMarkdownLinkDestination, EduMarkdownUriSchemaCompletionProvider())
    extend(CompletionType.BASIC, EduMarkdownPsiPatterns.toolWindowIdUriPath, EduMarkdownToolWindowIdCompletionContributor())
    extend(CompletionType.BASIC, EduMarkdownPsiPatterns.settingsIdUriPath, EduMarkdownSettingsIdCompletionContributor())
  }
}
