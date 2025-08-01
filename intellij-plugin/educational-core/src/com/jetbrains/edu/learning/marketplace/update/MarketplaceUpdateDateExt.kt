package com.jetbrains.edu.learning.marketplace.update

import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.courseConnector

data class CourseUpdateInfo(
  val courseVersion: Int,
  val formatVersion: Int,
)

fun EduCourse.getUpdateInfo(): CourseUpdateInfo? {
  if (id == 0 || !course.isMarketplace) return null
  return courseConnector.getLatestCourseUpdateInfo(id)
}