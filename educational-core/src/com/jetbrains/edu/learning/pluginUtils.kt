@file:JvmName("PluginUtils")

package com.jetbrains.edu.learning

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.ui.Messages
import com.intellij.util.text.VersionComparatorUtil

private const val KOTLIN_PLUGIN_ID = "org.jetbrains.kotlin"
private const val DEFAULT_KOTLIN_VERSION = "1.2.30"

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

fun pluginVersion(pluginId: String): String? = PluginManager.getPlugin(PluginId.getId(pluginId))?.version

fun kotlinPluginVersion(): String {
  val kotlinPluginVersion = pluginVersion(KOTLIN_PLUGIN_ID)?.takeWhile { it != '-' } ?: DEFAULT_KOTLIN_VERSION
  return VersionComparatorUtil.max(kotlinPluginVersion, DEFAULT_KOTLIN_VERSION)
}
