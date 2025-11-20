package com.jetbrains.edu.socialMedia

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.isUnitTestMode
import java.nio.file.Path
import kotlin.io.path.exists

object SocialMediaUtils {

  fun pluginRelativePath(path: String): Path? {
    require(!FileUtil.isAbsolute(path)) { "`$path` must not be an absolute path" }

    return findJetBrainsAcademyPlugin()
      ?.pluginPath
      ?.resolve(path)
      ?.takeIf { it.exists() }
  }

  private fun findJetBrainsAcademyPlugin(): IdeaPluginDescriptor? {
    return if (isUnitTestMode) {
      // In unit tests we use a bit different plugin id in test `plugin.xml`,
      // so the production variant doesn't work
      PluginManagerCore.loadedPlugins.singleOrNull { it.pluginId.idString.startsWith(EduNames.PLUGIN_ID) }
    }
    else {
      PluginManagerCore.getPlugin(PluginId.getId(EduNames.PLUGIN_ID))
    }
  }
}
