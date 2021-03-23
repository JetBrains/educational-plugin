package com.jetbrains.edu.learning.marketplace.update

import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector

/**
 * Returns remote course version if it's possible to update course, null otherwise
 */
fun EduCourse.getUpdateVersion(): Int? {
  if (id == 0 || !course.isMarketplace) return null
  val remoteCourseVersion = MarketplaceConnector.getInstance().getLatestCourseUpdateInfo(id)?.version ?: return null
  return if (marketplaceCourseVersion < remoteCourseVersion) remoteCourseVersion else null
}