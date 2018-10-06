package com.jetbrains.edu.java.learning.stepik.alt

import com.jetbrains.edu.learning.CoursesProvider
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.alt.courseFormat.HyperskillCourse

class JHyperskillCourseProvider : CoursesProvider {
  override fun loadCourses(): List<Course> {
    return listOf<Course>(HyperskillCourse("Hyperskill Course", "Hyperskill-JAVA"))
  }
}
