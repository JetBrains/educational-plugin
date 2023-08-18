package com.jetbrains.edu.learning.courseFormat

import com.jetbrains.edu.learning.courseFormat.EduFormatNames.COURSERA

class CourseraCourse : Course() {
  var submitManually = false

  override val itemType: String = COURSERA
}
