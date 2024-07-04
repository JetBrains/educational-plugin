package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.checkIsBackgroundThread
import com.jetbrains.edu.learning.codeforces.actions.StartCodeforcesContestAction
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.codeforces.newProjectUI.CodeforcesCoursesPanel
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CODEFORCES
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.CourseCreationInfo
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProviderFactory
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.onError
import kotlinx.coroutines.CoroutineScope
import javax.swing.Icon

class CodeforcesPlatformProviderFactory : CoursesPlatformProviderFactory {
  override fun getProviders(): List<CoursesPlatformProvider> = listOf(CodeforcesPlatformProvider())
}

class CodeforcesPlatformProvider : CoursesPlatformProvider() {
  override val name: String = CODEFORCES

  override val icon: Icon get() = EducationalCoreIcons.Codeforces

  override fun createPanel(scope: CoroutineScope, disposable: Disposable): CoursesPanel = CodeforcesCoursesPanel(this, scope, disposable)

  override fun joinAction(courseInfo: CourseCreationInfo, courseMode: CourseMode, coursePanel: CoursePanel) {
    StartCodeforcesContestAction.joinContest(courseInfo.course.id, coursePanel)
  }

  override suspend fun doLoadCourses(): List<CoursesGroup> {
    if (!isUnitTestMode) {
      checkIsBackgroundThread()
    }

    val document = CodeforcesConnector.getInstance().getContestsPage().onError {
      Logger.getInstance(CodeforcesPlatformProvider::class.java).error(it)
      return emptyList()
    }

    val contestsGroups = mutableListOf<CoursesGroup>()
    val upcomingContests = CodeforcesContestConnector.getUpcomingContests(document)
    val upcomingGroup = CoursesGroup(EduCoreBundle.message("course.dialog.codeforces.upcoming"), upcomingContests)
    contestsGroups.add(upcomingGroup)
    val recentContests = CodeforcesContestConnector.getRecentContests(document)
    val recentGroup = CoursesGroup(EduCoreBundle.message("course.dialog.codeforces.recent"), recentContests)
    contestsGroups.add(recentGroup)

    return contestsGroups
  }
}