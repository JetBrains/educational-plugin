package com.jetbrains.edu.ai.debugger.core.action

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.ai.debugger.core.EDU_AI_DEBUGGER_SERVICE_HOST_PROPERTY
import com.jetbrains.edu.ai.debugger.core.dialog.EduAIDebuggerServiceChangeHostDialog
import com.jetbrains.edu.learning.services.dialog.showDialogAndGetHost
import org.jetbrains.annotations.NonNls

class EduAIDebuggerServiceChangeHost : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val selectedUrl = EduAIDebuggerServiceChangeHostDialog().showDialogAndGetHost()
    if (selectedUrl == null) {
      LOG.warn("Selected AI Debugger service URL item is null")
      return
    }
    val existingValue = PropertiesComponent.getInstance().getValue(EDU_AI_DEBUGGER_SERVICE_HOST_PROPERTY)
    if (selectedUrl == existingValue) return

    PropertiesComponent.getInstance().setValue(EDU_AI_DEBUGGER_SERVICE_HOST_PROPERTY, selectedUrl, existingValue)
    LOG.info("AI Debugger service URL was changed to $selectedUrl")
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(EduAIDebuggerServiceChangeHost::class.java)

    @NonNls
    const val ACTION_ID: String = "Educational.EduAIDebuggerServiceChangeHost"
  }
}