package com.jetbrains.edu.markdown.taskDescription

import com.jetbrains.edu.codeInsight.taskDescription.EduToolWindowIdCompletionContributorBase

class EduMarkdownToolWindowIdCompletionContributor : EduToolWindowIdCompletionContributorBase() {
  override val elementTextPrefix: String = "//"
}
