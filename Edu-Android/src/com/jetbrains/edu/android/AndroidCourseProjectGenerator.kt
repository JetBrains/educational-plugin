package com.jetbrains.edu.android

import com.android.tools.idea.gradle.util.GradleProperties
import com.android.tools.idea.gradle.util.LocalProperties
import com.android.tools.idea.sdk.IdeSdks
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.gradle.generation.GradleCourseProjectGenerator

class AndroidCourseProjectGenerator(builder: AndroidCourseBuilder, course: Course) : GradleCourseProjectGenerator(builder, course) {

  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile) {
    super.createAdditionalFiles(project, baseDir)

    // We don't use template based creation for `local.properties` because
    // `java.util.Properties#store` does escaping before writing into a file.
    // It's important in case of Windows file path because they contains `\` and `:` which should be escaped.
    val localProperties = LocalProperties(project)
    localProperties.setAndroidSdkPath(IdeSdks.getInstance().androidSdkPath?.path ?: "")
    runThrowableAction(ThrowableRunnable { localProperties.save() })
    val gradleProperties = GradleProperties(project)
    gradleProperties.setJvmArgs("-Xmx1536m")
    runThrowableAction(ThrowableRunnable { gradleProperties.save() })
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(AndroidCourseProjectGenerator::class.java)

    private fun runThrowableAction(action: ThrowableRunnable<Throwable>) {
      try {
        action.run()
      } catch (e: Throwable) {
        LOG.warn(e)
      }
    }
  }
}
