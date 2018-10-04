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
private val DEFAULT_KOTLIN_VERSION = KotlinVersion("1.2.30", true)
private val KOTLIN_VERSION_PATTERN = """(\d+\.\d+(.\d+)?(-(eap-|M|rc-)\d+)?).*""".toRegex()

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

fun kotlinVersion(): KotlinVersion {
  val kotlinPluginVersion = pluginVersion(KOTLIN_PLUGIN_ID) ?: return DEFAULT_KOTLIN_VERSION
  val matchResult = KOTLIN_VERSION_PATTERN.matchEntire(kotlinPluginVersion) ?: return DEFAULT_KOTLIN_VERSION
  val version = matchResult.groupValues[1]
  val kotlinVersion = KotlinVersion(version, matchResult.groups[3] == null)
  return maxOf(kotlinVersion, DEFAULT_KOTLIN_VERSION)
}

data class KotlinVersion(val version: String, val isRelease: Boolean) : Comparable<KotlinVersion> {
  override fun compareTo(other: KotlinVersion): Int = VersionComparatorUtil.compare(version, other.version)
}
