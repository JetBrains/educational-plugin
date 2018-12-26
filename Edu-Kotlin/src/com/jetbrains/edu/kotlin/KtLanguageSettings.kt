package com.jetbrains.edu.kotlin

import com.jetbrains.edu.learning.DEFAULT_KOTLIN_VERSION
import com.jetbrains.edu.learning.KotlinVersion
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.gradle.JdkLanguageSettings
import com.jetbrains.edu.learning.kotlinVersion

class KtLanguageSettings : JdkLanguageSettings() {
  override fun getLanguageVersions() = listOf("1.2", "1.3")

  override fun validate(course: Course?): String? {
    course ?: return null
    val courseKotlinVersion = course.kotlinVersion
    val kotlinVersion = kotlinVersion()
    if (kotlinVersion < courseKotlinVersion) {
      return "Kotlin ${courseKotlinVersion.version} required. Try updating Kotlin plugin."
    }
    return super.validate(course)
  }

  private val Course.kotlinVersion: KotlinVersion
    get() {
      val langVersion = course.languageVersion ?: return DEFAULT_KOTLIN_VERSION
      return KotlinVersion(langVersion, true)
    }
}