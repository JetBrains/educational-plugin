package com.jetbrains.edu.learning.coursera.newProjectUI

import com.intellij.openapi.Disposable
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.COURSERA
import com.jetbrains.edu.learning.coursera.CourseraNames.COURSERA_HELP
import com.jetbrains.edu.learning.coursera.CourseraPlatformProvider
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import kotlinx.coroutines.CoroutineScope

class CourseraCoursesPanel(
  platformProvider: CourseraPlatformProvider,
  scope: CoroutineScope,
  disposable: Disposable
) : CoursesPanel(platformProvider, scope, disposable) {

  override fun tabDescription(): String {
    val linkText = """<a href="$COURSERA_HELP">$COURSERA</a>"""
    return EduCoreBundle.message("coursera.courses.explanation", linkText, COURSERA)
  }
}