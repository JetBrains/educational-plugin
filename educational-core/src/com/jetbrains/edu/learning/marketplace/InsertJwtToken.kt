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
import com.jetbrains.edu.learning.messages.EduCoreBundle

private const val LINK = "https://stgn.grazie.ai/auth/chrome/login/success"

@Suppress("ComponentNotRegistered")
class InsertJwtToken : DumbAwareAction(ACTION_TITLE) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val account = MarketplaceSettings.INSTANCE.hubAccount
    if (account == null) {
      CCUtils.showLoginNeededNotification(project, e.presentation.text) { MarketplaceConnector.getInstance().doAuthorize() }
      LOG.warn("User not logged in to Marketplace when inserting JWT token")
      return
    }

    val jwtToken = Messages.showInputDialog(EduCoreBundle.message("marketplace.inserted.jwt.token"), ACTION_TITLE, null) ?: return
    if (!MarketplaceSubmissionsConnector.getInstance().isJwtTokenValid(jwtToken)) {
      Messages.showErrorDialog(EduCoreBundle.message("marketplace.inserted.jwt.token.message", LINK),
                               EduCoreBundle.message("marketplace.inserted.jwt.token.error"))
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