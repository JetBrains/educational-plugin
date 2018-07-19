package com.jetbrains.edu.learning.intellij

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.edu.learning.EduConfigurator
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.intellij.generation.EduGradleUtils
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.io.IOException

abstract class GradleConfiguratorBase : EduConfigurator<JdkProjectSettings> {

  abstract override fun getCourseBuilder(): GradleCourseBuilderBase

  override fun excludeFromArchive(project: Project, path: String): Boolean {
    val pathSegments = FileUtil.splitPath(path)
    val name = pathSegments.last()
    if (GradleConstants.SETTINGS_FILE_NAME == name) {
      try {
        val settingsDefaultText = EduGradleUtils.getInternalTemplateText(courseBuilder.settingGradleTemplateName,
                                                                         courseBuilder.getConfigVariables(project))
        return FileUtil.loadFile(File(path)) == settingsDefaultText
      }
      catch (e: IOException) {
        LOG.error(e)
      }
    }
    return name in NAMES_TO_EXCLUDE || pathSegments.any { it in FOLDERS_TO_EXCLUDE } || "iml" == FileUtilRt.getExtension(name)
  }

  override fun getSourceDir(): String = EduNames.SRC
  override fun getTestDir(): String = EduNames.TEST

  override fun pluginRequirements(): List<String> = listOf("org.jetbrains.plugins.gradle", "JUnit")

  companion object {
    private val NAMES_TO_EXCLUDE = ContainerUtil.newHashSet(
      ".idea", "EduTestRunner.java", "gradlew", "gradlew.bat", "local.properties", "gradle.properties",
      GradleConstants.SETTINGS_FILE_NAME, "gradle-wrapper.jar", "gradle-wrapper.properties")

    private val FOLDERS_TO_EXCLUDE = ContainerUtil.newHashSet(EduNames.OUT, EduNames.BUILD)

    private val LOG = Logger.getInstance(GradleConfiguratorBase::class.java)
  }
}
