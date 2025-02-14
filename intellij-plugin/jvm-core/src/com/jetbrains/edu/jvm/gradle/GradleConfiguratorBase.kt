package com.jetbrains.edu.jvm.gradle

import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.jvmEnvironmentSettings
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.configuration.attributesEvaluator.AttributesEvaluator
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_JAR
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_PROPERTIES
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_UNIX
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_WIN
import com.jetbrains.edu.learning.gradle.GradleConstants.LOCAL_PROPERTIES
import com.jetbrains.edu.learning.gradle.GradleConstants.SETTINGS_GRADLE

abstract class GradleConfiguratorBase : EduConfigurator<JdkProjectSettings> {
  abstract override val courseBuilder: GradleCourseBuilderBase

  override val courseFileAttributesEvaluator: AttributesEvaluator = AttributesEvaluator(super.courseFileAttributesEvaluator) {
    file(SETTINGS_GRADLE) {
      includeIntoArchive()
    }

    dir(*FOLDERS_TO_EXCLUDE) {
      excludeFromArchive()

      any {
        excludeFromArchive()
      }
    }

    file(*NAMES_TO_EXCLUDE) {
      excludeFromArchive()
    }
  }

  override val sourceDir: String
    get() = EduNames.SRC

  override val testDirs: List<String>
    get() = listOf(EduNames.TEST)

  override val pluginRequirements: List<PluginId>
    get() = listOf(PluginId.getId("org.jetbrains.plugins.gradle"), PluginId.getId("JUnit"))

  companion object {
    private val NAMES_TO_EXCLUDE = arrayOf(
      "EduTestRunner.java", GRADLE_WRAPPER_UNIX, GRADLE_WRAPPER_WIN, LOCAL_PROPERTIES,
      GRADLE_WRAPPER_JAR, GRADLE_WRAPPER_PROPERTIES
    )

    private val FOLDERS_TO_EXCLUDE = arrayOf(EduNames.OUT, EduNames.BUILD, GRADLE)
  }

  override fun getEnvironmentSettings(project: Project): Map<String, String> = jvmEnvironmentSettings(project)
}
