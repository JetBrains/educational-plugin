package com.jetbrains.edu.learning.coursera.newProjectUI

import com.intellij.openapi.Disposable
import com.jetbrains.edu.learning.coursera.CourseraNames.COURSERA
import com.jetbrains.edu.learning.coursera.CourseraNames.COURSERA_HELP
import com.jetbrains.edu.learning.coursera.CourseraPlatformProvider
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.TabInfo
import kotlinx.coroutines.CoroutineScope

class CourseraCoursesPanel(
  platformProvider: CourseraPlatformProvider,
  scope: CoroutineScope,
  disposable: Disposable
) : CoursesPanel(platformProvider, scope, disposable) {

  override fun tabInfo(): TabInfo {
    val linkText = """<a href="$COURSERA_HELP">$COURSERA</a>"""
    val infoText = EduCoreBundle.message("coursera.courses.explanation", linkText, COURSERA)
    return TabInfo(infoText)
  }
}