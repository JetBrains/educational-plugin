package com.jetbrains.edu.learning.marketplace

import com.intellij.icons.AllIcons
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProviderFactory
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

  override suspend fun loadCourses(): List<CoursesGroup> {
    val marketplaceConnector = MarketplaceConnector.getInstance()
    val courses = marketplaceConnector.searchCourses()
    return CoursesGroup(courses).asList()
  }
}

