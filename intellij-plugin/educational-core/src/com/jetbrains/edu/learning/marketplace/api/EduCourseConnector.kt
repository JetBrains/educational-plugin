package com.jetbrains.edu.learning.marketplace.api

import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.update.CourseUpdateInfo
import com.jetbrains.edu.learning.statistics.DownloadCourseContext

// TODO(make methods suspend)
interface EduCourseConnector {
  fun getCourseIdFromLink(link: String): Int

  fun getCourseInfoByLink(link: String): EduCourse?

  fun getLatestCourseUpdateInfo(courseId: Int): CourseUpdateInfo?

  fun searchCourse(courseId: Int, searchPrivate: Boolean = false): EduCourse?

  fun loadCourseStructure(course: EduCourse, downloadContext: DownloadCourseContext)
  fun loadCourse(courseId: Int, downloadContext: DownloadCourseContext): EduCourse
}