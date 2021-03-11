package com.jetbrains.edu.learning.stepik.course

import com.jetbrains.edu.learning.courseFormat.EduCourse

interface CourseConnector {
  fun getCourseIdFromLink(link: String): Int

  fun getCourseInfoByLink(link: String): EduCourse?
}