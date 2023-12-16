package com.jetbrains.edu.learning.marketplace.update

import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.UpdateInfo


fun EduCourse.getUpdateInfo(): UpdateInfo? {
  if (id == 0 || !course.isMarketplace) return null
  return MarketplaceConnector.getInstance().getLatestCourseUpdateInfo(id)
}