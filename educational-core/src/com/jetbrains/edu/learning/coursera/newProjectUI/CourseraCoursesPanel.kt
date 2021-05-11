package com.jetbrains.edu.learning.coursera.newProjectUI

import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.coursera.CourseraNames.COURSERA
import com.jetbrains.edu.learning.coursera.CourseraNames.COURSERA_HELP
import com.jetbrains.edu.learning.coursera.CourseraPlatformProvider
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.LinkInfo
import com.jetbrains.edu.learning.newproject.ui.TabInfo
import kotlinx.coroutines.CoroutineScope

class CourseraCoursesPanel(platformProvider: CourseraPlatformProvider, scope: CoroutineScope) : CoursesPanel(platformProvider, scope) {

  override fun tabInfo(): TabInfo {
    val linkText = """<a href="$COURSERA_HELP">$COURSERA</a>"""
    val infoText = EduCoreBundle.message("coursera.courses.explanation", linkText)
    return TabInfo(infoText)
  }
}