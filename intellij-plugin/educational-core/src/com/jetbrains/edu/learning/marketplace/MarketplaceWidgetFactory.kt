package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.jetbrains.edu.learning.LoginWidgetFactory
import com.jetbrains.edu.learning.RemoteEnvHelper
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle

class MarketplaceWidgetFactory : LoginWidgetFactory() {
  override val widgetId: String = "widget.marketplace"

  override fun isWidgetAvailable(course: Course): Boolean {
    if (RemoteEnvHelper.isRemoteDevServer()) {
      // widget should not be shown on remote development because there is currently no authentication logic implemented.
      // Instead, we use a workaround by reading the JBA UID token from the local file system (see EDU-6321)
      return false
    }
    return course is EduCourse && course.isMarketplace
  }

  override fun getDisplayName(): String = EduCoreBundle.message("marketplace.widget.title")

  override fun createWidget(project: Project): StatusBarWidget = MarketplaceWidget(project)
}