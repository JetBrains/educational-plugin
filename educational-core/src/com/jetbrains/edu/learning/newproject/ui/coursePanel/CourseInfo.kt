package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.EduProjectSettings

data class CourseInfo(val course: Course,
                      val location: () -> String? = { null },
                      val languageSettings: () -> LanguageSettings<*>? = { null }) {
  val projectSettings: EduProjectSettings? get() = languageSettings()?.getSettings()
}