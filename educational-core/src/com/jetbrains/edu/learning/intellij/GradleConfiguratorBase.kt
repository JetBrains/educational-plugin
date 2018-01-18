package com.jetbrains.edu.learning.intellij

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.edu.learning.EduConfigurator
import com.jetbrains.edu.learning.EduNames

abstract class GradleConfiguratorBase : EduConfigurator<JdkProjectSettings> {

  override fun excludeFromArchive(path: String): Boolean {
    val pathSegments = FileUtil.splitPath(path)
    val name = pathSegments.last()
    return name in NAMES_TO_EXCLUDE || pathSegments.any { it in FOLDERS_TO_EXCLUDE } || "iml" == FileUtilRt.getExtension(name)
  }

  override fun getSourceDir(): String = EduNames.SRC
  override fun getTestDir(): String = EduNames.TEST

  companion object {
    private val NAMES_TO_EXCLUDE = ContainerUtil.newHashSet(
      ".idea", "EduTestRunner.java", "gradlew", "gradlew.bat", "local.properties", "gradle.properties",
      "settings.gradle", "gradle-wrapper.jar", "gradle-wrapper.properties")

    private val FOLDERS_TO_EXCLUDE = ContainerUtil.newHashSet(EduNames.OUT, EduNames.BUILD)
  }
}
