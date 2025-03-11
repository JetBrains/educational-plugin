package com.jetbrains.edu.markdown.taskDescription

import com.jetbrains.edu.codeInsight.taskDescription.EduSettingsIdCompletionContributorBase

class EduMarkdownSettingsIdCompletionContributor : EduSettingsIdCompletionContributorBase() {
  override val elementTextPrefix: String = "//"
}
