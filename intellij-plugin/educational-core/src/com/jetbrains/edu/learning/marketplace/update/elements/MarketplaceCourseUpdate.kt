package com.jetbrains.edu.learning.marketplace.update.elements

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.update.elements.CourseUpdate
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

class MarketplaceCourseUpdate(
  override val localItem: EduCourse,
  override val remoteItem: EduCourse
) : CourseUpdate<EduCourse>(localItem, remoteItem) {
  override suspend fun update(project: Project) {
    baseUpdate(project)
    localItem.marketplaceCourseVersion = remoteItem.marketplaceCourseVersion
    localItem.updateDate = remoteItem.updateDate

    YamlFormatSynchronizer.saveItemWithRemoteInfo(localItem)
  }
}