package com.jetbrains.edu.jvm

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import org.jetbrains.annotations.VisibleForTesting

private const val JVM_LANGUAGE_LEVEL = "jvm_language_level"

val Course.minJvmSdkVersion: ParsedJavaVersion
  get() = ParsedJavaVersion.fromStringLanguageLevel(environmentSettings[JVM_LANGUAGE_LEVEL])

val Course.maxJvmSdkVersion: ParsedJavaVersion?
  get() = when(this) {
    is HyperskillCourse -> JavaVersionParseSuccess(JavaSdkVersion.JDK_21)
    else -> null
  }

fun jvmEnvironmentSettings(project: Project): Map<String, String> = mapOf(
  JVM_LANGUAGE_LEVEL to LanguageLevelProjectExtension.getInstance(project).languageLevel.toString()
)

@VisibleForTesting
fun Course.setLanguageLevel(languageLevel: String?) {
  if (languageLevel == null) {
    course.environmentSettings = course.environmentSettings.minus(JVM_LANGUAGE_LEVEL)
  }
  else {
    course.environmentSettings = course.environmentSettings.plus(JVM_LANGUAGE_LEVEL to languageLevel)
  }
}