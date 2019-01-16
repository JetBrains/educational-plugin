package com.jetbrains.edu.learning.stepik.api

import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.courseFormat.CourseCompatibility
import com.jetbrains.edu.learning.courseFormat.CourseVisibility
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.stepik.StepikConnector.FEATURED_COURSES
import com.jetbrains.edu.learning.stepik.StepikUtils

object StepikNewConnectorUtils {

  fun getAvailableCourses(coursesList: CoursesList): List<EduCourse> {
    coursesList.courses.forEach { info ->
      StepikUtils.setCourseLanguage(info)
    }
    val availableCourses = coursesList.courses.filter {
      !StringUtil.isEmptyOrSpaces(it.type)
      && it.compatibility != CourseCompatibility.UNSUPPORTED
    }

    availableCourses.forEach { it ->
      it.visibility = StepikNewConnectorUtils.getVisibility(it)
    }
    return availableCourses
  }

  private fun getVisibility(course: EduCourse): CourseVisibility {
    return when {
      !course.isPublic -> CourseVisibility.PrivateVisibility
      FEATURED_COURSES.contains(course.id) -> CourseVisibility.FeaturedVisibility(FEATURED_COURSES.indexOf(course.id))
      FEATURED_COURSES.isEmpty() -> CourseVisibility.LocalVisibility
      else -> CourseVisibility.PublicVisibility
    }
  }

}
