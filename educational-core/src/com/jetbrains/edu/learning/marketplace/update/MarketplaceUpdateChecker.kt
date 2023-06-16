package com.jetbrains.edu.learning.marketplace.update

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.checkForUpdates
import com.jetbrains.edu.learning.update.CourseUpdateChecker

@Service
class MarketplaceUpdateChecker(project: Project) : CourseUpdateChecker(project) {

  override fun courseCanBeUpdated(): Boolean {
    val marketplaceCourse = course as? EduCourse ?: return false
    return marketplaceCourse.isMarketplaceRemote || marketplaceCourse.isStudy && marketplaceCourse.isMarketplace
  }

  override fun doCheckIsUpToDate(onFinish: () -> Unit) {
    val marketplaceCourse = course as? EduCourse ?: return

    marketplaceCourse.checkForUpdates(project, false, onFinish)
  }

  companion object {
    fun getInstance(project: Project): MarketplaceUpdateChecker {
      return project.service()
    }
  }
}
