package com.jetbrains.edu.learning.coursera

import com.jetbrains.edu.learning.courseFormat.Course

class CourseraCourse : Course() {
  var submitManually = false

  override val itemType: String = CourseraNames.COURSERA
}
