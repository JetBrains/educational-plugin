package com.jetbrains.edu.learning.marketplace.awsTracks.changeHost

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.services.dialog.showDialogAndGetHost
import org.jetbrains.annotations.NonNls

class AWSTracksServiceChangeHost : DumbAwareAction(EduCoreBundle.message("aws.tracks.service.change.host")) {

  override fun actionPerformed(e: AnActionEvent) {
    val selectedUrl = AWSTracksServiceChangeHostDialog().showDialogAndGetHost()
    if (selectedUrl == null) {
      LOG.warn("Selected AWS Tracks service URL item is null")
      return
    }

    val existingValue = AWSTracksServiceHost.getSelectedUrl()
    if (selectedUrl == existingValue) return

    AWSTracksServiceHost.setUrl(selectedUrl, existingValue)
    LOG.info("AWS Tracks service URL was changed to $selectedUrl")
  }

  companion object {
    private val LOG: Logger = logger<AWSTracksServiceChangeHost>()

    @NonNls
    const val ACTION_ID = "Educational.Student.AWSTracksServiceChangeHost"
  }
}