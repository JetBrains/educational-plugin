package com.jetbrains.edu.learning.intellij

import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.util.PathUtil
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.edu.learning.EduConfigurator

abstract class GradleConfiguratorBase : EduConfigurator<JdkProjectSettings> {

  override fun excludeFromArchive(path: String): Boolean {
    val name = PathUtil.getFileName(path)
    return name in NAMES_TO_EXCLUDE || path.contains("build") || "iml" == FileUtilRt.getExtension(name)
  }

  companion object {
    private val NAMES_TO_EXCLUDE = ContainerUtil.newHashSet(
      "out", "build", ".idea", "EduTestRunner.java",
      "gradlew", "gradlew.bat", "local.properties", "gradle.properties",
      "build.gradle", "settings.gradle", "gradle-wrapper.jar", "gradle-wrapper.properties")
  }
}
