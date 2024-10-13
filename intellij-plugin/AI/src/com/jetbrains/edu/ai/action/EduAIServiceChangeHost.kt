package com.jetbrains.edu.ai.action

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.ai.EDU_AI_SERVICE_HOST_PROPERTY
import com.jetbrains.edu.ai.dialog.EduAIServiceChangeHostDialog
import com.jetbrains.edu.ai.host.EduAIServiceHost.STAGING
import com.jetbrains.edu.learning.services.dialog.showDialogAndGetHost
import org.jetbrains.annotations.NonNls

class EduAIServiceChangeHost : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val selectedUrl = EduAIServiceChangeHostDialog().showDialogAndGetHost()
    if (selectedUrl == null) {
      LOG.warn("Selected AI service URL item is null")
      return
    }
    val existingValue = PropertiesComponent.getInstance().getValue(EDU_AI_SERVICE_HOST_PROPERTY, STAGING.url)
    if (selectedUrl == existingValue) return

    PropertiesComponent.getInstance().setValue(EDU_AI_SERVICE_HOST_PROPERTY, selectedUrl, existingValue)
    LOG.info("AI service URL was changed to $selectedUrl")
  }

  companion object {
    private val LOG: Logger = thisLogger()

    @NonNls
    const val ACTION_ID: String = "Educational.EduAIServiceChangeHost"
  }

}