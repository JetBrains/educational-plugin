package com.jetbrains.edu.socialMedia

import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.learning.findJetBrainsAcademyPlugin
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
}
