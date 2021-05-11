package com.jetbrains.edu.learning.codeforces.newProjectUI

import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.StartCodeforcesContestAction
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.*
import kotlinx.coroutines.CoroutineScope

class CodeforcesCoursesPanel(platformProvider: CoursesPlatformProvider, scope: CoroutineScope) : CoursesPanel(platformProvider, scope) {
  override fun toolbarAction(): ToolbarActionWrapper {
    return ToolbarActionWrapper(EduCoreBundle.lazyMessage("codeforces.open.contest.by.link"),
                                StartCodeforcesContestAction(showViewAllLabel = false))
  }

  override fun tabInfo(): TabInfo {
    val linkText = """<a href="${CodeforcesNames.CODEFORCES_URL}">${CodeforcesNames.CODEFORCES_TITLE}</a>"""
    return TabInfo(EduCoreBundle.message("codeforces.courses.description", linkText))
  }
}