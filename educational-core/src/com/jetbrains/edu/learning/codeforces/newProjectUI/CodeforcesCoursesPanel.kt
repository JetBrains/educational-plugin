package com.jetbrains.edu.learning.codeforces.newProjectUI

import com.intellij.openapi.actionSystem.AnAction
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.StartCodeforcesContestAction
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.LinkInfo
import com.jetbrains.edu.learning.newproject.ui.TabInfo
import kotlinx.coroutines.CoroutineScope

class CodeforcesCoursesPanel(platformProvider: CoursesPlatformProvider, scope: CoroutineScope) : CoursesPanel(platformProvider, scope) {
  override fun toolbarAction(): AnAction? {
    return StartCodeforcesContestAction(EduCoreBundle.lazyMessage("codeforces.open.contest.by.link"), false)
  }

  override fun tabInfo(): TabInfo? {
    val linkInfo = LinkInfo(EduCoreBundle.message("course.dialog.go.to.website"), CodeforcesNames.CODEFORCES_URL)
    return TabInfo(EduCoreBundle.message("codeforces.courses.description", CodeforcesNames.CODEFORCES), linkInfo)
  }
}