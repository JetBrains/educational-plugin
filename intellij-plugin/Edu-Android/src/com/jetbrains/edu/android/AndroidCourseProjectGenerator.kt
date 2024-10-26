package com.jetbrains.edu.android

import com.android.SdkConstants
import com.android.tools.idea.gradle.util.GradleWrapper
import com.android.tools.idea.sdk.IdeSdks
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.jvm.gradle.generation.EduGradleUtils
import com.jetbrains.edu.jvm.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_PROPERTIES
import com.jetbrains.edu.learning.gradle.GradleConstants.LOCAL_PROPERTIES
import com.jetbrains.edu.learning.isUnitTestMode
import org.jetbrains.plugins.gradle.settings.DistributionType
import java.io.IOException
import java.util.*

class AndroidCourseProjectGenerator(builder: AndroidCourseBuilder, course: Course) : GradleCourseProjectGenerator(builder, course) {

  override fun createAdditionalFiles(holder: CourseInfoHolder<Course>, isNewCourse: Boolean) {
    super.createAdditionalFiles(holder, isNewCourse)

    // fileSystem can be different here in tests.
    // But `GradleWrapper.create` works properly only for local file system
    if (holder.courseDir.fileSystem == LocalFileSystem.getInstance()) {
      invokeAndWaitIfNeeded {
        GradleWrapper.create(holder.courseDir, GradleWrapper.getGradleVersionToUse(), null)
      }
    }
    // We have to create property files manually
    // instead of `com.android.tools.idea.gradle.util.LocalProperties` and `com.android.tools.idea.gradle.util.LocalProperties`
    // because they work only with `java.io.File` but in tests we use in memory file system
    mapOf(SdkConstants.SDK_DIR_PROPERTY to (IdeSdks.getInstance().androidSdkPath?.path ?: ""))
      .saveAsPropertyFile(holder.courseDir, LOCAL_PROPERTIES)
    mapOf(
      // https://developer.android.com/jetpack/androidx#using_androidx_libraries_in_your_project
      "android.useAndroidX" to "true",
      "android.enableJetifier" to "true",
      "org.gradle.jvmargs" to "-Xmx1536m"
    ).saveAsPropertyFile(holder.courseDir, GRADLE_PROPERTIES)
  }

  override fun setupGradleSettings(project: Project, sdk: Sdk?) {
    EduGradleUtils.setGradleSettings(project, sdk, project.basePath!!, DistributionType.DEFAULT_WRAPPED)
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(AndroidCourseProjectGenerator::class.java)

    private fun Map<String, String>.saveAsPropertyFile(baseDir: VirtualFile, name: String) {
      // We don't use template based creation for properties files because
      // `java.util.Properties#store` does escaping before writing into a file.
      // It's important in case of Windows file path because they contains `\` and `:` which should be escaped.
      val properties = toSortedProperties()
      try {
        GeneratorUtils.runInWriteActionAndWait {
          val child = baseDir.findChild(name)
          if (child == null) {
            val propertiesFile = baseDir.createChildData(AndroidCourseBuilder::class.java, name)
            propertiesFile.getOutputStream(AndroidCourseBuilder::class.java).use { properties.store(it, "") }
          }
        }
      } catch (e: IOException) {
        // We want to fail tests if exception happened
        val log: (String, Throwable) -> Unit = if (isUnitTestMode) LOG::error else LOG::warn
        log("Failed to create `$name`", e)
      }
    }

    private fun Map<String, String>.toSortedProperties(): Properties =
      SortedProperties().apply { putAll(this@toSortedProperties) }
  }

  private class SortedProperties : Properties() {
    @Synchronized
    override fun keys(): Enumeration<Any> {
      return Collections.enumeration(super.keys().toList().sortedBy(Any::toString))
    }
  }
}
