package com.jetbrains.edu.socialMedia

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.learning.EduNames
import java.nio.file.Path
import kotlin.io.path.exists

object SocialMediaUtils {

  fun pluginRelativePath(path: String): Path? {
    require(!FileUtil.isAbsolute(path)) { "`$path` must not be an absolute path" }

    return PluginManagerCore.getPlugin(PluginId.getId(EduNames.PLUGIN_ID))
      ?.pluginPath
      ?.resolve(path)
      ?.takeIf { it.exists() }
  }
}