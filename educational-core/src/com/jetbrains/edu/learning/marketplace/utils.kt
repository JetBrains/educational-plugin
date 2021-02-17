package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

private const val DATA_DELIMITER = ";"
private const val DELIMITER = "."

fun decodeHubToken(token: String): String? {
  val parts = token.split(DATA_DELIMITER)
  if (parts.size != 2) {
    error("Hub oauth token data part is malformed")
  }
  val userData = parts[0].split(DELIMITER)
  if (userData.size != 4) {
    error("Hub oauth token data part is malformed")
  }
  return if (userData[2].isEmpty()) null else userData[2]
}

fun Course.generateCourseItemsIds() {
  visitSections { section -> section.generateId() }
  visitLessons { lesson ->
    lesson.visitTasks { task ->
      task.generateId()
    }
    lesson.generateId()
  }
  YamlFormatSynchronizer.saveRemoteInfo(this)
}

fun Course.setRemoteMarketplaceCourseVersion() {
  val updateInfo = MarketplaceConnector.getInstance().getLatestCourseUpdateInfo(marketplaceId)
  if (updateInfo != null) {
    incrementMarketplaceCourseVersion(updateInfo.version)
  }
}

fun StudyItem.isMarketplaceRemoteCourse(): Boolean = this is EduCourse && marketplaceId > 0