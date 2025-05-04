package com.jetbrains.edu.html.taskDescription

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType

class EduHtmlCompletionContributor : CompletionContributor() {

  init {
    extend(CompletionType.BASIC, EduHtmlPsiPatterns.inHrefAttributeValue, EduHtmlUriSchemaCompletionProvider())
    extend(CompletionType.BASIC, EduHtmlPsiPatterns.toolWindowIdUriPath, EduHtmlToolWindowIdCompletionContributor())
    extend(CompletionType.BASIC, EduHtmlPsiPatterns.settingsIdUriPath, EduHtmlSettingsIdCompletionContributor())
  }
}
