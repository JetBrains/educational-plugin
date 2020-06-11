package com.jetbrains.edu.learning.codeforces

import com.jetbrains.edu.learning.checkIsBackgroundThread
import com.jetbrains.edu.learning.codeforces.newProjectUI.CodeforcesCoursesPanel
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.newproject.ui.*
import icons.EducationalCoreIcons
import javax.swing.Icon

class CodeforcesPlatformProviderFactory : CoursesPlatformProviderFactory {
  override fun getProviders(): List<CoursesPlatformProvider> = listOf(CodeforcesPlatformProvider())
}

private class CodeforcesPlatformProvider : CoursesPlatformProvider {
  override val name: String = CodeforcesNames.CODEFORCES.capitalize()

  override val icon: Icon get() = EducationalCoreIcons.Codeforces.to24()

  override fun getPanel(dialog: BrowseCoursesDialog): CoursesPanel = CodeforcesCoursesPanel(dialog, this)

  override suspend fun loadCourses(): List<Course> {
    checkIsBackgroundThread()
    val taskTextLanguage = CodeforcesSettings.getInstance().preferableTaskTextLanguage ?: TaskTextLanguage.ENGLISH
    return if (isUnitTestMode) emptyList() else CodeforcesContestLoader.getContestInfos(locale = taskTextLanguage.locale)
  }
}