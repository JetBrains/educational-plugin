package com.jetbrains.edu.learning.compatibility

import com.intellij.ide.plugins.InstalledPluginsState
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.compatibilityProvider
import com.jetbrains.edu.learning.courseFormat.ext.configurator
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

    private fun Course.pluginCompatibility(): CourseCompatibility? {
      val requiredPlugins = compatibilityProvider?.requiredPlugins() ?: return null
      // TODO: O(requiredPlugins * allPlugins) because PluginManager.getPlugin takes O(allPlugins).
      //  Can be improved at least to O(requiredPlugins * log(allPlugins))
      val loadedPlugins = PluginManager.getLoadedPlugins()
      val notLoadedPlugins = requiredPlugins
        .mapNotNull {
          // BACKCOMPAT: 2019.3. Use `PluginManagerCore#getPlugin` instead
          @Suppress("DEPRECATION")
          val pluginDescriptor = PluginManager.getPlugin(it.id)
          if (pluginDescriptor == null || pluginDescriptor !in loadedPlugins) {
            it to pluginDescriptor
          }
          else {
            null
          }
        }

      val pluginsState = InstalledPluginsState.getInstance()
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
