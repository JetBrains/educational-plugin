package com.jetbrains.edu.learning.stepik.course

import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.statistics.DownloadCourseContext

interface CourseConnector {
  fun getCourseIdFromLink(link: String): Int

  fun getCourseInfoByLink(link: String): EduCourse?

  fun searchCourse(courseId: Int, searchPrivate: Boolean = false): EduCourse?

  fun loadCourseStructure(course: EduCourse, downloadContext: DownloadCourseContext)
  fun loadCourse(courseId: Int, downloadContext: DownloadCourseContext): EduCourse
}