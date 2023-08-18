package com.jetbrains.edu.jvm.gradle

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.jvmEnvironmentSettings
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_JAR
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_PROPERTIES
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_UNIX
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_WIN
import com.jetbrains.edu.learning.gradle.GradleConstants.LOCAL_PROPERTIES
import com.jetbrains.edu.learning.gradle.GradleConstants.SETTINGS_GRADLE
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import java.io.File
import java.io.IOException

abstract class GradleConfiguratorBase : EduConfigurator<JdkProjectSettings> {
  abstract override val courseBuilder: GradleCourseBuilderBase

  override fun excludeFromArchive(project: Project, course: Course, file: VirtualFile): Boolean {
    if (super.excludeFromArchive(project, course, file)) return true
    val name = file.name
    val path = file.path
    val pathSegments = path.split(VfsUtilCore.VFS_SEPARATOR_CHAR)
    if (SETTINGS_GRADLE == name) {
      // Always upload (don't exclude) settings.gradle for Hyperskill CC (EDU-4750)
      if (course is HyperskillCourse) return false

      try {
        val settingsDefaultText = GeneratorUtils.getInternalTemplateText(courseBuilder.settingGradleTemplateName(course),
                                                                         courseBuilder.templateVariables(project.name))
        val ioFile = File(path)
        return if (ioFile.exists()) FileUtil.loadFile(ioFile) == settingsDefaultText else true
      }
      catch (e: IOException) {
        LOG.error(e)
      }
    }
    return name in NAMES_TO_EXCLUDE || pathSegments.any { it in FOLDERS_TO_EXCLUDE }
  }

  override val sourceDir: String
    get() = EduNames.SRC

  override val testDirs: List<String>
    get() = listOf(EduNames.TEST)

  override val pluginRequirements: List<PluginId>
    get() = listOf(PluginId.getId("org.jetbrains.plugins.gradle"), PluginId.getId("JUnit"))

  companion object {
    private val NAMES_TO_EXCLUDE = ContainerUtil.newHashSet(
      "EduTestRunner.java", GRADLE_WRAPPER_UNIX, GRADLE_WRAPPER_WIN, LOCAL_PROPERTIES,
      SETTINGS_GRADLE, GRADLE_WRAPPER_JAR, GRADLE_WRAPPER_PROPERTIES
    )

    private val FOLDERS_TO_EXCLUDE = ContainerUtil.newHashSet(EduNames.OUT, EduNames.BUILD, GRADLE)

    private val LOG = Logger.getInstance(GradleConfiguratorBase::class.java)
  }

  override fun getEnvironmentSettings(project: Project): Map<String, String> = jvmEnvironmentSettings(project)
}
