package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.application.ex.ApplicationUtil
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.checkIsBackgroundThread
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.codeforces.newProjectUI.CodeforcesCoursesPanel
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProviderFactory
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseMode
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.onError
import icons.EducationalCoreIcons
import kotlinx.coroutines.CoroutineScope
import javax.swing.Icon

class CodeforcesPlatformProviderFactory : CoursesPlatformProviderFactory {
  override fun getProviders(): List<CoursesPlatformProvider> = listOf(CodeforcesPlatformProvider())
}

class CodeforcesPlatformProvider : CoursesPlatformProvider() {
  override val name: String = CodeforcesNames.CODEFORCES.capitalize()

  override val icon: Icon get() = EducationalCoreIcons.Codeforces

  override fun createPanel(scope: CoroutineScope): CoursesPanel = CodeforcesCoursesPanel(this, scope)

  override fun joinAction(courseInfo: CourseInfo, courseMode: CourseMode, coursePanel: CoursePanel) {
    val codeforcesCourse = StartCodeforcesContestAction.startContest(courseInfo.course.id) ?: error("Cannot load course ${courseInfo.course.name} ${courseInfo.course.id}")
    StartCodeforcesContestAction.showCourseInfo(codeforcesCourse)
  }

  override suspend fun doLoadCourses(): List<CoursesGroup> {
    if (!isUnitTestMode) {
      checkIsBackgroundThread()
    }

    val document = CodeforcesConnector.getInstance().getContestsPage().onError {
      Logger.getInstance(CodeforcesPlatformProvider::class.java).error(it)
      return emptyList()
    }
    val upcomingContests = CodeforcesContestConnector.getUpcomingContests(document)
    val recentContests = CodeforcesContestConnector.getRecentContests(document)
    val upcomingGroup = CoursesGroup(EduCoreBundle.message("course.dialog.codeforces.upcoming"), upcomingContests)
    val recentGroup = CoursesGroup(EduCoreBundle.message("course.dialog.codeforces.recent"), recentContests)

    return listOf(upcomingGroup, recentGroup)
  }
}