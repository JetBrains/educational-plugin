package com.jetbrains.edu.learning

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.ui.Messages

fun getDisabledPlugins(ids: List<String>): List<String> {
  val disabledPluginIds = PluginManager.getDisabledPlugins().toSet()
  return ids.filter { it in disabledPluginIds }
}

fun enablePlugins(ids: List<String>) {
  for (id in ids) {
    PluginManager.enablePlugin(id)
  }
  restartIDE("Required plugins were enabled")
}

fun restartIDE(messageInfo: String) {
  val ideName = ApplicationNamesInfo.getInstance().fullProductName
  val restartInfo = if (ApplicationManager.getApplication().isRestartCapable) "$ideName will be restarted" else "Restart $ideName"
  val message = "$messageInfo. $restartInfo in order to apply changes"

  Messages.showInfoMessage(message, restartInfo)
  ApplicationManagerEx.getApplicationEx().restart(true)
}
