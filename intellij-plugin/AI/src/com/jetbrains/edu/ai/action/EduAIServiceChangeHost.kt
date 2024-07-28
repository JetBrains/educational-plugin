package com.jetbrains.edu.ai.action

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.ai.EDU_AI_SERVICE_HOST_PROPERTY
import com.jetbrains.edu.ai.dialog.showDialogAndGetAIServiceHost
import com.jetbrains.edu.ai.host.EduAIServiceHost.PRODUCTION
import org.jetbrains.annotations.NonNls

class EduAIServiceChangeHost : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val selectedUrl = showDialogAndGetAIServiceHost()
    if (selectedUrl == null) {
      LOG.warn("Selected AI service URL item is null")
      return
    }
    val existingValue = PropertiesComponent.getInstance().getValue(EDU_AI_SERVICE_HOST_PROPERTY, PRODUCTION.url)
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