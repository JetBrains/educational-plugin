package com.jetbrains.edu.go

import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.LanguageSettings

class GoCourseBuilder : EduCourseBuilder<GoProjectSettings> {
  override fun getLanguageSettings(): LanguageSettings<GoProjectSettings> = GoLanguageSettings()
}
