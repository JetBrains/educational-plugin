package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.application.runInEdt
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.InlineBanner
import com.jetbrains.edu.learning.authUtils.AuthorizationPlace
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle

class NotLoggedInInlineBanner : InlineBanner(EditorNotificationPanel.Status.Warning) {
  init {
    setMessage(EduCoreBundle.message("marketplace.submissions.not.logged.in.banner.message"))
    addAction(EduCoreBundle.message("marketplace.submissions.not.logged.in.banner.login.action")) {
      MarketplaceConnector.getInstance().doAuthorize(
        { runInEdt { removeFromParent() } },
        authorizationPlace = AuthorizationPlace.TASK_DESCRIPTION_HEADER
      )
    }
  }
}
