package com.jetbrains.edu.jvm

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import org.jetbrains.annotations.VisibleForTesting

private const val JVM_LANGUAGE_LEVEL = "jvm_language_level"

// TODO(refactor this), this is a temporary solution
// All JVM-based Hyperskill courses now require JDK version 21
val hyperskillJdkVersion: JavaSdkVersion = JavaSdkVersion.JDK_21

val Course.minJvmSdkVersion: ParsedJavaVersion
  get() = when (this) {
    is HyperskillCourse -> JavaVersionParseSuccess(hyperskillJdkVersion)
    else -> ParsedJavaVersion.fromStringLanguageLevel(environmentSettings[JVM_LANGUAGE_LEVEL])
  }

val Course.maxJvmSdkVersion: ParsedJavaVersion?
  get() = when(this) {
    is HyperskillCourse -> JavaVersionParseSuccess(hyperskillJdkVersion)
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