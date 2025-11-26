package com.jetbrains.edu.learning.submissions.ui.linkHandler

import com.intellij.openapi.project.Project
import com.intellij.ui.ColorUtil
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.submissions.ui.SubmissionsTab.Companion.SUBMISSION_PROTOCOL
import com.jetbrains.edu.learning.submissions.ui.SubmissionsTab.Companion.textStyleHeader
import com.jetbrains.edu.learning.taskToolWindow.links.SwingToolWindowLinkHandler
import com.jetbrains.edu.learning.ui.EduColors

class LoginLinkHandler(
  project: Project,
  private val submissionsManager: SubmissionsManager
) : SwingToolWindowLinkHandler(project) {

  override fun process(url: String, referUrl: String?): Boolean = when {
    url.startsWith(SUBMISSION_LOGIN_URL) -> {
      submissionsManager.doAuthorize()
      true
    }

    else -> false
  }

  companion object {
    private const val SUBMISSION_LOGIN_URL = "${SUBMISSION_PROTOCOL}login/"

    fun getLoginText(): String {
      val hyperlinkColor = ColorUtil.toHex(EduColors.hyperlinkColor)
      val linkText = EduCoreBundle.message("submissions.tab.login")
      return "<a $textStyleHeader;color:#$hyperlinkColor href=$SUBMISSION_LOGIN_URL>$linkText</a>"
    }
  }
}
