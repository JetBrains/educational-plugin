package com.jetbrains.edu.learning.codeforces

import com.jetbrains.edu.learning.checkIsBackgroundThread
import com.jetbrains.edu.learning.codeforces.newProjectUI.CodeforcesCoursesPanel
import com.jetbrains.edu.learning.compatibility.CourseCompatibility
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProviderFactory
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.asList
import icons.EducationalCoreIcons
import kotlinx.coroutines.CoroutineScope
import javax.swing.Icon

class CodeforcesPlatformProviderFactory : CoursesPlatformProviderFactory {
  override fun getProviders(): List<CoursesPlatformProvider> = listOf(CodeforcesPlatformProvider())
}

private class CodeforcesPlatformProvider : CoursesPlatformProvider() {
  override val name: String = CodeforcesNames.CODEFORCES.capitalize()

  override val icon: Icon get() = EducationalCoreIcons.Codeforces

  override fun createPanel(scope: CoroutineScope): CoursesPanel = CodeforcesCoursesPanel(this, scope)

  override suspend fun loadCourses(): List<CoursesGroup> {
    checkIsBackgroundThread()
    val taskTextLanguage = CodeforcesSettings.getInstance().preferableTaskTextLanguage ?: TaskTextLanguage.ENGLISH
    return if (isUnitTestMode) {
      emptyList()
    }
    else {
      val courses = CodeforcesContestLoader.getContestInfos(locale = taskTextLanguage.locale).filter {
        val compatibility = it.compatibility
        compatibility == CourseCompatibility.Compatible || compatibility is CourseCompatibility.PluginsRequired
      }

      CoursesGroup(courses).asList()
    }
  }
}