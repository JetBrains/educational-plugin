package com.jetbrains.edu.html.taskDescription

import com.jetbrains.edu.codeInsight.taskDescription.EduSettingsIdCompletionContributorBase
import com.jetbrains.edu.learning.taskToolWindow.links.TaskDescriptionLinkProtocol

class EduHtmlSettingsIdCompletionContributor : EduSettingsIdCompletionContributorBase() {
  override val elementTextPrefix: String = TaskDescriptionLinkProtocol.SETTINGS.protocol
}
