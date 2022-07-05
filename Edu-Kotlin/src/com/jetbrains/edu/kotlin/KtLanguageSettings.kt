package com.jetbrains.edu.kotlin

import com.jetbrains.edu.jvm.JdkLanguageSettings
import com.jetbrains.edu.kotlin.messages.EduKotlinBundle
import com.jetbrains.edu.learning.DEFAULT_KOTLIN_VERSION
import com.jetbrains.edu.learning.KotlinVersion
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.kotlinVersion
import com.jetbrains.edu.learning.newproject.ui.ValidationMessage

class KtLanguageSettings : JdkLanguageSettings() {
  override fun validate(course: Course?, courseLocation: String?): ValidationMessage? {
    if (course != null) {
      val courseKotlinVersion = course.kotlinVersion
      val kotlinVersion = kotlinVersion()
      if (kotlinVersion < courseKotlinVersion) {
        return ValidationMessage(EduKotlinBundle.message("update.kotlin.plugin", courseKotlinVersion.version))
      }
    }
    return super.validate(course, courseLocation)
  }

  private val Course.kotlinVersion: KotlinVersion
    get() {
      val langVersion = course.languageVersion ?: return DEFAULT_KOTLIN_VERSION
      return KotlinVersion(langVersion)
    }
}