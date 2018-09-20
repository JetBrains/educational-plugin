package com.jetbrains.edu.android

import com.android.SdkConstants
import com.android.tools.idea.sdk.IdeSdks
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.gradle.generation.GradleCourseProjectGenerator
import java.io.IOException
import java.util.*

class AndroidCourseProjectGenerator(builder: AndroidCourseBuilder, course: Course) : GradleCourseProjectGenerator(builder, course) {

  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile) {
    super.createAdditionalFiles(project, baseDir)

    // We don't use template based creation for `local.properties` because
    // `java.util.Properties#store` does escaping before writing into a file.
    // It's important in case of Windows file path because they contains `\` and `:` which should be escaped.
    val props = Properties()
    props.setProperty(SdkConstants.SDK_DIR_PROPERTY, IdeSdks.getInstance().androidSdkPath?.path ?: "")

    try {
      GeneratorUtils.runInWriteActionAndWait(ThrowableComputable {
        val localProperties = baseDir.createChildData(AndroidCourseBuilder::class.java, SdkConstants.FN_LOCAL_PROPERTIES)
        localProperties.getOutputStream(AndroidCourseBuilder::class.java).use { props.store(it, "") }
      })
    } catch (e: IOException) {
      LOG.warn("Failed to create `${SdkConstants.FN_LOCAL_PROPERTIES}`", e)
    }
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(AndroidCourseProjectGenerator::class.java)
  }
}
