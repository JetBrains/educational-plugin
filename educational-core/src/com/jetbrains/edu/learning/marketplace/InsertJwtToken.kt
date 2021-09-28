package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings

@Suppress("ComponentNotRegistered")
class InsertJwtToken: DumbAwareAction(ACTION_TITLE) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val account = MarketplaceSettings.INSTANCE.account
    if (account == null) {
      CCUtils.showLoginNeededNotification(project, e.presentation.text) { MarketplaceConnector.getInstance().doAuthorize() }
        LOG.warn("User not logged in to Marketplace when inserting JWT token")
        return
    }

    val jwtToken = Messages.showInputDialog("JWT token:", ACTION_TITLE, null) ?: return
    if (!MarketplaceSubmissionsConnector.getInstance().isJwtTokenValid(jwtToken)) {
      Messages.showErrorDialog("Inserted JWT token is invalid. Please obtain a new one <a href=\"https://stgn.grazie.ai/auth/chrome/login/success\">here</a>",
                               "Inserted JWT token is invalid")
      return
    }
    account.saveJwtToken(jwtToken)
    LOG.info("Successfully inserted JWT token: $jwtToken")
    ApplicationManager.getApplication().messageBus.syncPublisher(MarketplaceSubmissionsConnector.GRAZIE_AUTHORIZATION_TOPIC).userLoggedIn()
  }

  companion object {
    private const val ACTION_TITLE = "Insert JWT token"
    val LOG = logger<InsertJwtToken>()
  }
}