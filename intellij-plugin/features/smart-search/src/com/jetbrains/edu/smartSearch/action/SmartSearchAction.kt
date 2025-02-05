package com.jetbrains.edu.smartSearch.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.smartSearch.connector.SmartSearchManager
import com.jetbrains.edu.smartSearch.ui.SmartSearchDialog
import kotlinx.coroutines.launch

@Suppress("ComponentNotRegistered")
class SmartSearchAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val smartSearchDialogResult = SmartSearchDialog().showAndGetWithSearchQuery() ?: return
    currentThreadCoroutineScope().launch {
      SmartSearchManager.getInstance().searchAndShow(smartSearchDialogResult)
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}