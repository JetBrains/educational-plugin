package com.jetbrains.edu.learning.compatibility

import capitalize
import com.intellij.ide.plugins.InstalledPluginsState
import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.compatibilityProvider
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.newproject.ui.getRequiredPluginsMessage
import com.jetbrains.edu.learning.plugins.PluginInfo
import com.jetbrains.edu.learning.stepik.StepikNames

sealed class CourseCompatibility {
  object Compatible : CourseCompatibility()
  object IncompatibleVersion : CourseCompatibility()
  object Unsupported : CourseCompatibility()
  class PluginsRequired(val toInstallOrEnable: List<PluginInfo>) : CourseCompatibility()

  companion object {

    private val LOG: Logger = Logger.getInstance(CourseCompatibility::class.java)

    @JvmStatic
    fun forCourse(courseInfo: Course): CourseCompatibility {
      if (courseInfo is JetBrainsAcademyCourse) {
        return Compatible
      }

      // @formatter:off
      return courseInfo.versionCompatibility() ?:
             courseInfo.pluginCompatibility() ?:
             courseInfo.configuratorCompatibility() ?:
             Compatible
      // @formatter:on
    }

    private fun Course.versionCompatibility(): CourseCompatibility? {
      if (this !is EduCourse) return null

      val typeLanguage = StringUtil.split(type, " ")
      if (typeLanguage.size < 2) {
        return Unsupported
      }
      val prefix = typeLanguage[0]
      if (!prefix.startsWith(StepikNames.PYCHARM_PREFIX)) {
        return Unsupported
      }

      val versionString = prefix.substring(StepikNames.PYCHARM_PREFIX.length)
      if (versionString.isEmpty()) return null
      try {
        val version = Integer.valueOf(versionString)
        if (version > JSON_FORMAT_VERSION) IncompatibleVersion
      }
      catch (e: NumberFormatException) {
        LOG.info("Wrong version format", e)
        return Unsupported
      }

      return null
    }

    // projectLanguage parameter should be passed only for hyperskill courses because for Hyperskill
    // it can differ from the course.programmingLanguage
    fun Course.validateLanguage(projectLanguage: String = programmingLanguage): Result<Unit, String> {
      val pluginCompatibility = pluginCompatibility()
      if (pluginCompatibility is PluginsRequired) {
        return Err(getRequiredPluginsMessage(pluginCompatibility.toInstallOrEnable))
      }

      if (configurator == null) {
        return Err(EduCoreBundle.message("rest.service.language.not.supported", ApplicationNamesInfo.getInstance().productName,
                                         projectLanguage.capitalize()))
      }
      return Ok(Unit)
    }

    fun Course.pluginCompatibility(): CourseCompatibility? {
      val requiredPlugins = mutableListOf<PluginInfo>()
      compatibilityProvider?.requiredPlugins()?.let { requiredPlugins.addAll(it) }
      for (pluginInfo in course.pluginDependencies) {
        if (requiredPlugins.find { it.id.idString == pluginInfo.stringId } != null) {
          continue
        }
        requiredPlugins.add(pluginInfo)
      }

      if (requiredPlugins.isEmpty()) {
        return null
      }

      val pluginsState = InstalledPluginsState.getInstance()

      // TODO: O(requiredPlugins * allPlugins) because PluginManager.getPlugin takes O(allPlugins).
      //  Can be improved at least to O(requiredPlugins * log(allPlugins))
      val loadedPlugins = PluginManager.getLoadedPlugins()
      val notLoadedPlugins = requiredPlugins
        .mapNotNull {
          if (pluginsState.wasInstalledWithoutRestart(it.id)) {
            return@mapNotNull null
          }

          val pluginDescriptor = PluginManagerCore.getPlugin(it.id)
          if (pluginDescriptor == null || pluginDescriptor !in loadedPlugins) {
            it to pluginDescriptor
          }
          else {
            null
          }
        }

      val toInstallOrEnable = notLoadedPlugins.filter { (info, pluginDescriptor) ->
        // Plugin is just installed and not loaded by IDE (i.e. it requires restart)
        pluginDescriptor == null && !pluginsState.wasInstalled(info.id) ||
        // Plugin is installed but disabled
        pluginDescriptor?.isEnabled == false
      }

      return if (notLoadedPlugins.isNotEmpty())  PluginsRequired(toInstallOrEnable.map { it.first }) else null
    }

    private fun Course.configuratorCompatibility(): CourseCompatibility? {
      return if (configurator == null) Unsupported else null
    }
  }
}
