package com.jetbrains.edu.learning.submissions.ui.linkHandler

import com.intellij.openapi.project.Project
import com.intellij.ui.ColorUtil
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.agreement.UserAgreementSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.submissions.SolutionSharingPreference
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.submissions.ui.SubmissionsTab.Companion.SUBMISSION_PROTOCOL
import com.jetbrains.edu.learning.submissions.ui.SubmissionsTab.Companion.textStyleHeader
import com.jetbrains.edu.learning.taskToolWindow.links.SwingToolWindowLinkHandler
import com.jetbrains.edu.learning.ui.EduColors

class EnableSolutionSharingLinkHandler(project: Project) : SwingToolWindowLinkHandler(project) {
  override fun process(url: String, referUrl: String?): Boolean = when {
    url.startsWith(SOLUTION_SHARING_URL) -> {
      UserAgreementSettings.getInstance().setSolutionSharing(SolutionSharingPreference.ALWAYS)
      val task = project.getCurrentTask()
      if (task != null) {
        SubmissionsManager.getInstance(project).loadCommunitySubmissions(task)
      }
      true
    }

    else -> false
  }

  companion object {
    fun enableSolutionSharingString(): String =
      "<a $textStyleHeader;color:#${ColorUtil.toHex(EduColors.hyperlinkColor)} href=$SOLUTION_SHARING_URL>${EduCoreBundle.message("submissions.tab.sharing")}</a>"

    private const val SOLUTION_SHARING_URL = "${SUBMISSION_PROTOCOL}sharing/"
  }
}