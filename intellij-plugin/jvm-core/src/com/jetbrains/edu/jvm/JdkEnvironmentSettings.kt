package com.jetbrains.edu.jvm

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.DefaultEnvironmentSettings
import com.jetbrains.edu.learning.courseFormat.EnvironmentSettingKey
import com.jetbrains.edu.learning.courseFormat.EnvironmentSettingValue
import com.jetbrains.edu.learning.courseFormat.defaultEnvironmentSettings
import com.jetbrains.edu.learning.courseFormat.getEnvironmentSetting
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.jvmcompat.GradleJvmSupportMatrix

val JVM_LANGUAGE_LEVEL = EnvironmentSettingKey("jvm_language_level")

val minCCJdkVersion: JavaSdkVersion by lazy { guessCCMinJdkVersion() }

val Course.minJvmSdkVersion: ParsedJavaVersion
  get() = when {
    courseMode == CourseMode.EDUCATOR -> JavaVersionParseSuccess(minCCJdkVersion)
    else -> ParsedJavaVersion.fromStringLanguageLevel(getEnvironmentSetting(JVM_LANGUAGE_LEVEL))
  }

fun jvmEnvironmentSettings(project: Project): DefaultEnvironmentSettings {
  val languageLevel: String = LanguageLevelProjectExtension.getInstance(project).languageLevel.toString()
  return defaultEnvironmentSettings(JVM_LANGUAGE_LEVEL to EnvironmentSettingValue.user(languageLevel))
}

private fun guessCCMinJdkVersion(): JavaSdkVersion {
  val defaultGradleVersion = GradleVersion.current()
  val defaultJavaVersion = GradleJvmSupportMatrix.suggestOldestSupportedJavaVersion(defaultGradleVersion) ?: return JavaSdkVersion.JDK_17
  return JavaSdkVersion.fromJavaVersion(defaultJavaVersion) ?: JavaSdkVersion.JDK_17
}
