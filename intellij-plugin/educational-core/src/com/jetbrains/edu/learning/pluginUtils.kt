@file:JvmName("PluginUtils")

package com.jetbrains.edu.learning

import com.intellij.externalDependencies.DependencyOnPlugin
import com.intellij.externalDependencies.ExternalDependenciesManager
import com.intellij.ide.plugins.PluginEnabler
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.installAndEnable
import com.intellij.util.text.VersionComparatorUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.compatibilityProvider
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jsoup.Jsoup

const val KOTLIN_PLUGIN_ID = "org.jetbrains.kotlin"
val DEFAULT_KOTLIN_VERSION = KotlinVersion("1.9.0")
private val KOTLIN_VERSION_PATTERN = """(\d+-)((?<version>\d+\.\d+(.\d+)?(-(RC|RC2|M1|M2))?)(-release-\d+)?).*""".toRegex()

fun getDisabledPlugins(ids: List<PluginId>): List<PluginId> {
  return ids.filter { PluginManagerCore.isDisabled(it) }
}

private fun restartIDE(messageInfo: String) {
  val ideName = ApplicationNamesInfo.getInstance().fullProductName
  val restartInfo = if (ApplicationManager.getApplication().isRestartCapable) {
    EduCoreBundle.message("ide.restart.info.title", ideName)
  }
  else {
    EduCoreBundle.message("ide.restart.message.title", ideName)
  }

  val message = if (ApplicationManager.getApplication().isRestartCapable) {
    "$messageInfo. ${EduCoreBundle.message("ide.restart.info.message", ideName)}"
  }
  else {
    "$messageInfo. ${EduCoreBundle.message("ide.restart.message", ideName)}"
  }

  Messages.showInfoMessage(message, restartInfo)
  ApplicationManagerEx.getApplicationEx().restart(true)
}

fun pluginVersion(pluginId: String): String? = PluginManagerCore.getPlugin(PluginId.getId(pluginId))?.version

// Kotlin plugin keeps the latest supported language version only in change notes
fun kotlinVersionFromPlugin(pluginId: String): String? {
  val changeNotes = PluginManagerCore.getPlugin(PluginId.getId(pluginId))?.changeNotes ?: return null
  return Jsoup.parse(changeNotes).getElementsByTag("h3").firstOrNull()?.text()
}

fun kotlinVersion(): KotlinVersion {
  val kotlinPluginVersion = kotlinVersionFromPlugin(KOTLIN_PLUGIN_ID) ?: return DEFAULT_KOTLIN_VERSION
  val matchResult = KOTLIN_VERSION_PATTERN.matchEntire(kotlinPluginVersion) ?: return DEFAULT_KOTLIN_VERSION
  val version = matchResult.groups["version"]?.value ?: return DEFAULT_KOTLIN_VERSION
  val kotlinVersion = KotlinVersion(version)
  return maxOf(kotlinVersion, DEFAULT_KOTLIN_VERSION)
}

data class KotlinVersion(val version: String) : Comparable<KotlinVersion> {
  override fun compareTo(other: KotlinVersion): Int = VersionComparatorUtil.compare(version, other.version)
}

fun setUpPluginDependencies(project: Project, course: Course) {
  val allDependencies = course.pluginDependencies.map { DependencyOnPlugin(it.stringId, it.minVersion, it.maxVersion) }.toMutableList()

  course.compatibilityProvider?.requiredPlugins()?.forEach { plugin ->
    if (allDependencies.none { plugin.stringId == it.pluginId }) {
      allDependencies.add(DependencyOnPlugin(plugin.stringId, null, null))
    }
  }

  ExternalDependenciesManager.getInstance(project).setAllDependencies(allDependencies)
}

fun installAndEnablePlugin(pluginIds: Set<PluginId>, onSuccess: Runnable) = installAndEnable(null, pluginIds, true, onSuccess = onSuccess)

fun enablePlugins(pluginsId: List<PluginId>) {
  val descriptors = pluginsId.mapNotNull { pluginId -> PluginManagerCore.getPlugin(pluginId) }
  @Suppress("UnstableApiUsage")
  PluginEnabler.HEADLESS.enable(descriptors)
  restartIDE(EduCoreBundle.message("required.plugin.were.enabled"))
}

