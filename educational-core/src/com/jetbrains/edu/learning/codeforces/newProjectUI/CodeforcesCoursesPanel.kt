package com.jetbrains.edu.learning.codeforces.newProjectUI

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
    val linkInfo = LinkInfo(EduCoreBundle.message("course.dialog.go.to.website"), CodeforcesNames.CODEFORCES_URL)
    return TabInfo(EduCoreBundle.message("codeforces.courses.description", CodeforcesNames.CODEFORCES), linkInfo)
  }
}