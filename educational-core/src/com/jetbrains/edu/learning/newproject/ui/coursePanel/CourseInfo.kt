package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course

data class CourseInfo(val course: Course, val location: () -> String?, val languageSettings: () -> LanguageSettings<*>?) {
  val projectSettings = languageSettings()?.settings
}