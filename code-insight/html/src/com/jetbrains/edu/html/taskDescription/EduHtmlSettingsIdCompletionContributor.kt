package com.jetbrains.edu.html.taskDescription

import com.jetbrains.edu.codeInsight.taskDescription.EduSettingsIdCompletionContributorBase
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskDescriptionLinkProtocol

class EduHtmlSettingsIdCompletionContributor : EduSettingsIdCompletionContributorBase() {
  override val elementTextPrefix: String = TaskDescriptionLinkProtocol.SETTINGS.protocol
}
