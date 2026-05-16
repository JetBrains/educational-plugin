package com.jetbrains.edu.jvm

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.jvmcompat.GradleJvmSupportMatrix

const val JVM_LANGUAGE_LEVEL = "jvm_language_level"

val minCCJdkVersion: JavaSdkVersion by lazy { guessCCMinJdkVersion() }

val Course.minJvmSdkVersion: ParsedJavaVersion
  get() = when {
    courseMode == CourseMode.EDUCATOR -> JavaVersionParseSuccess(minCCJdkVersion)
    else -> ParsedJavaVersion.fromStringLanguageLevel(environmentSettings[JVM_LANGUAGE_LEVEL])
  }

fun jvmEnvironmentSettings(project: Project): Map<String, String> = mapOf(
  JVM_LANGUAGE_LEVEL to LanguageLevelProjectExtension.getInstance(project).languageLevel.toString()
)

private fun guessCCMinJdkVersion(): JavaSdkVersion {
  val defaultGradleVersion = GradleVersion.current()
  val defaultJavaVersion = GradleJvmSupportMatrix.suggestOldestSupportedJavaVersion(defaultGradleVersion) ?: return JavaSdkVersion.JDK_17
  return JavaSdkVersion.fromJavaVersion(defaultJavaVersion) ?: JavaSdkVersion.JDK_17
}
