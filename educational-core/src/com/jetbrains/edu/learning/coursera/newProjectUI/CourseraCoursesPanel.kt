package com.jetbrains.edu.learning.coursera.newProjectUI

import com.jetbrains.edu.learning.coursera.CourseraNames
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
    val linkText = COURSERA
    val infoText = EduCoreBundle.message("coursera.courses.explanation", COURSERA).dropWhile { it in linkText }
    val linkInfo = LinkInfo(linkText, COURSERA_HELP)
    return TabInfo(infoText, linkInfo)
  }
}