package com.jetbrains.edu.learning.stepik.alt

import com.jetbrains.edu.learning.CoursesProvider
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.alt.courseFormat.HyperskillCourse

class HyperskillCourseProvider : CoursesProvider {
  override fun loadCourses(): List<Course> {
    return listOf<Course>(HyperskillCourse("Adaptive JAVA", "Hyperskill-JAVA"))
  }
}
