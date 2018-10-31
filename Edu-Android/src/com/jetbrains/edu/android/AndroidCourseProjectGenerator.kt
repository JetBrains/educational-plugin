package com.jetbrains.edu.android

import com.android.SdkConstants
import com.android.tools.idea.sdk.IdeSdks
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.gradle.generation.EduGradleUtils
import com.jetbrains.edu.learning.gradle.generation.GradleCourseProjectGenerator
import org.jetbrains.plugins.gradle.settings.DistributionType
import java.io.IOException

class AndroidCourseProjectGenerator(builder: AndroidCourseBuilder, course: Course) : GradleCourseProjectGenerator(builder, course) {

  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile) {
    super.createAdditionalFiles(project, baseDir)

    // We have to create property files manually
    // instead of `com.android.tools.idea.gradle.util.LocalProperties` and `com.android.tools.idea.gradle.util.LocalProperties`
    // because they work only with `java.io.File` but in tests we use in memory file system
    mapOf(SdkConstants.SDK_DIR_PROPERTY to (IdeSdks.getInstance().androidSdkPath?.path ?: ""))
      .saveAsPropertyFile(baseDir, SdkConstants.FN_LOCAL_PROPERTIES)
    mapOf("org.gradle.jvmargs" to "-Xmx1536m")
      .saveAsPropertyFile(baseDir, SdkConstants.FN_GRADLE_PROPERTIES)
  }

  override fun setupGradleSettings(project: Project) {
    EduGradleUtils.setGradleSettings(project, project.basePath!!, DistributionType.DEFAULT_WRAPPED)
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(AndroidCourseProjectGenerator::class.java)

    private fun Map<String, String>.saveAsPropertyFile(baseDir: VirtualFile, name: String) {
      // We don't use template based creation for properties files because
      // `java.util.Properties#store` does escaping before writing into a file.
      // It's important in case of Windows file path because they contains `\` and `:` which should be escaped.
      val properties = toProperties()
      try {
        GeneratorUtils.runInWriteActionAndWait(ThrowableComputable {
          val propertiesFile = baseDir.createChildData(AndroidCourseBuilder::class.java, name)
          propertiesFile.getOutputStream(AndroidCourseBuilder::class.java).use { properties.store(it, "") }
        })
      } catch (e: IOException) {
        LOG.warn("Failed to create `$name`", e)
      }
    }
  }
}
