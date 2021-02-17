package com.jetbrains.edu.learning.marketplace

import com.intellij.icons.AllIcons
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.computeUnderProgress
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProviderFactory
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseMode
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.asList
import kotlinx.coroutines.CoroutineScope
import javax.swing.Icon

class MarketplacePlatformProviderFactory : CoursesPlatformProviderFactory {
  override fun getProviders(): List<CoursesPlatformProvider> =
    if (isFeatureEnabled(EduExperimentalFeatures.MARKETPLACE)) listOf (MarketplacePlatformProvider()) else emptyList()
}

private class MarketplacePlatformProvider : CoursesPlatformProvider() {

  override val name: String = MARKETPLACE

  // TODO: replace with icon from designers
  override val icon: Icon get() = AllIcons.Actions.Stub

  override fun createPanel(scope: CoroutineScope): CoursesPanel = MarketplaceCoursesPanel(this, scope)

  override fun joinAction(courseInfo: CourseInfo, courseMode: CourseMode, coursePanel: CoursePanel) {
    val course = courseInfo.course
    if (course is EduCourse && course.isMarketplace) {
      computeUnderProgress(title = EduCoreBundle.message("marketplace.loading.course")) {
        MarketplaceConnector.getInstance().loadCourseStructure(course)
      }
      super.joinAction(courseInfo, courseMode, coursePanel)
    }
  }

  override suspend fun loadCourses(): List<CoursesGroup> {
    val marketplaceConnector = MarketplaceConnector.getInstance()
    val courses = marketplaceConnector.searchCourses()
    return CoursesGroup(courses).asList()
  }
}
