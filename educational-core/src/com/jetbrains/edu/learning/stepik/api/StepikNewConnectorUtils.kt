package com.jetbrains.edu.learning.stepik.api

import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.courseFormat.CourseCompatibility
import com.jetbrains.edu.learning.courseFormat.CourseVisibility
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.stepik.featuredCourses
import com.jetbrains.edu.learning.stepik.setCourseLanguage

fun getAvailableCourses(coursesList: CoursesList): List<EduCourse> {
  coursesList.courses.forEach { info ->
    setCourseLanguage(info)
  }
  val availableCourses = coursesList.courses.filter {
    !StringUtil.isEmptyOrSpaces(it.type)
    && it.compatibility != CourseCompatibility.UNSUPPORTED
  }

  availableCourses.forEach { it ->
    it.visibility = getVisibility(it)
  }
  return availableCourses
}

private fun getVisibility(course: EduCourse): CourseVisibility {
  return when {
    !course.isPublic -> CourseVisibility.PrivateVisibility
    featuredCourses.contains(course.id) -> CourseVisibility.FeaturedVisibility(featuredCourses.indexOf(course.id))
    featuredCourses.isEmpty() -> CourseVisibility.LocalVisibility
    else -> CourseVisibility.PublicVisibility
  }
}

