package com.jetbrains.edu.csharp

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.newCourseSettings.NewCourseSettings
import com.jetbrains.edu.learning.newproject.newCourseSettings.NewCourseSettingsCatalog

data class CSharpNewCourseSettings(val languageVersion: String) : NewCourseSettings {
  override fun applyToCourse(course: Course) {
    course.languageVersion = languageVersion
  }
}

object CSharpNewCourseSettingsCatalog : NewCourseSettingsCatalog.List<CSharpNewCourseSettings> {
  override val configs: List<CSharpNewCourseSettings>
    get() = supportedDotNetVersions().map(::CSharpNewCourseSettings)

  override val preferred: CSharpNewCourseSettings
    get() = configs.firstOrNull { it.languageVersion == DEFAULT_DOT_NET } ?: CSharpNewCourseSettings(DEFAULT_DOT_NET)
}
