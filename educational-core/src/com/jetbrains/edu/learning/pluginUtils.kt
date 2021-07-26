@file:JvmName("PluginUtils")

package com.jetbrains.edu.learning

import com.intellij.externalDependencies.DependencyOnPlugin
import com.intellij.externalDependencies.ExternalDependenciesManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.util.text.VersionComparatorUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.compatibilityProvider

private const val KOTLIN_PLUGIN_ID = "org.jetbrains.kotlin"
val DEFAULT_KOTLIN_VERSION = KotlinVersion("1.4.10", true)
private val KOTLIN_VERSION_PATTERN = """(\d+\.\d+(.\d+)?(-(eap-|M|rc-)\d+)?).*""".toRegex()

fun getDisabledPlugins(ids: List<PluginId>): List<PluginId> {
  return ids.filter { PluginManagerCore.isDisabled(it) }
}

fun restartIDE(messageInfo: String) {
  val ideName = ApplicationNamesInfo.getInstance().fullProductName
  val restartInfo = if (ApplicationManager.getApplication().isRestartCapable) "$ideName will be restarted" else "Restart $ideName"
  val message = "$messageInfo. $restartInfo in order to apply changes"

  Messages.showInfoMessage(message, restartInfo)
  ApplicationManagerEx.getApplicationEx().restart(true)
}

fun pluginVersion(pluginId: String): String? = PluginManagerCore.getPlugin(PluginId.getId(pluginId))?.version

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

fun setUpPluginDependencies(project: Project, course: Course) {
  val allDependencies = course.pluginDependencies.map { DependencyOnPlugin(it.id, it.minVersion, it.maxVersion) }.toMutableList()

  course.compatibilityProvider?.requiredPlugins()?.forEach { plugin ->
    if (allDependencies.none { plugin.stringId == it.pluginId }) {
      allDependencies.add(DependencyOnPlugin(plugin.stringId, null, null))
    }
  }

  ExternalDependenciesManager.getInstance(project).setAllDependencies(allDependencies)
}
