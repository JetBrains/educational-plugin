package com.jetbrains.edu.android

import com.android.SdkConstants
import com.android.tools.idea.gradle.util.GradleWrapper
import com.android.tools.idea.sdk.IdeSdks
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.jvm.gradle.generation.EduGradleUtils
import com.jetbrains.edu.jvm.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.InMemoryTextualContents
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_PROPERTIES
import com.jetbrains.edu.learning.gradle.GradleConstants.LOCAL_PROPERTIES
import org.jetbrains.plugins.gradle.settings.DistributionType
import java.io.StringWriter
import java.util.*

class AndroidCourseProjectGenerator(builder: AndroidCourseBuilder, course: Course) : GradleCourseProjectGenerator(builder, course) {

  override fun createAdditionalFiles(holder: CourseInfoHolder<Course>) {
    super.createAdditionalFiles(holder)

    // fileSystem can be different here in tests.
    // But `GradleWrapper.create` works properly only for local file system
    if (holder.courseDir.fileSystem == LocalFileSystem.getInstance()) {
      invokeAndWaitIfNeeded {
        GradleWrapper.create(holder.courseDir, GradleWrapper.getGradleVersionToUse(), null)
      }
    }
  }

  override fun autoCreatedAdditionalFiles(holder: CourseInfoHolder<Course>): List<EduFile> =
    super.autoCreatedAdditionalFiles(holder) + listOfNotNull(
      // We have to create property files manually
      // instead of `com.android.tools.idea.gradle.util.LocalProperties` and `com.android.tools.idea.gradle.util.GradleProperties`
      // because they work only with `java.io.File` but in tests we use in memory file system
      mapOf(SdkConstants.SDK_DIR_PROPERTY to (IdeSdks.getInstance().androidSdkPath?.path ?: ""))
        .toPropertyFileAsEduFile(holder.courseDir, LOCAL_PROPERTIES),
      mapOf(
        // https://developer.android.com/jetpack/androidx#using_androidx_libraries_in_your_project
        "android.useAndroidX" to "true",
        "android.enableJetifier" to "true",
        "org.gradle.jvmargs" to "-Xmx1536m"
      ).toPropertyFileAsEduFile(holder.courseDir, GRADLE_PROPERTIES)
    )

  override fun setupGradleSettings(project: Project, sdk: Sdk?) {
    EduGradleUtils.setGradleSettings(project, sdk, project.basePath!!, DistributionType.DEFAULT_WRAPPED)
  }

  companion object {
    private fun Map<String, String>.toPropertyFileAsEduFile(baseDir: VirtualFile, name: String): EduFile? {
      // We don't use template based creation for properties files because
      // `java.util.Properties#store` does escaping before writing into a file.
      // It's important in case of Windows file path because they contains `\` and `:` which should be escaped.
      val properties = toSortedProperties()
      val child = baseDir.findChild(name)
      if (child != null) return null

      val propertiesWriter = StringWriter()
      propertiesWriter.use { writer ->
        properties.store(writer, "")
      }
      val propertiesContents = propertiesWriter.toString()
      return EduFile(name, InMemoryTextualContents(propertiesContents))
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
