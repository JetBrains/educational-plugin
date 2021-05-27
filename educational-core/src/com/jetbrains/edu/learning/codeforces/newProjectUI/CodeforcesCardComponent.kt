package com.jetbrains.edu.learning.codeforces.newProjectUI

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.CourseCardComponent

class CodeforcesCardComponent(course: Course) : CourseCardComponent(course) {
  override fun isLogoVisible(): Boolean {
    return false
  }
}