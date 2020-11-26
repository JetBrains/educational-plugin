package com.jetbrains.edu.learning.marketplace

import com.intellij.icons.AllIcons
import com.jetbrains.edu.learning.marketplace.api.Graphql
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.QueryData
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProviderFactory
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.asList
import kotlinx.coroutines.CoroutineScope
import javax.swing.Icon

class MarketplacePlatformProviderFactory : CoursesPlatformProviderFactory {
  override fun getProviders(): List<CoursesPlatformProvider> = listOf(MarketplacePlatformProvider())
}

private class MarketplacePlatformProvider : CoursesPlatformProvider() {

  override val name: String = MARKETPLACE

  // TODO: replace with icon from designers
  override val icon: Icon get() = AllIcons.Actions.Stub

  override fun createPanel(scope: CoroutineScope): CoursesPanel = MarketplaceCoursesPanel(this, scope)

  override suspend fun loadCourses(): List<CoursesGroup> {
    val marketplaceConnector = MarketplaceConnector.getInstance()
    val queryData = QueryData(Graphql().getSearchQuery())
    val courses = marketplaceConnector.searchCourses(queryData)
    return CoursesGroup(courses).asList()
  }
}

