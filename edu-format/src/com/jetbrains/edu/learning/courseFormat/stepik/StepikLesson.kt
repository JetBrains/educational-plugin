package com.jetbrains.edu.learning.courseFormat.stepik

import com.jetbrains.edu.learning.courseFormat.EduFormatNames.STEPIK
import com.jetbrains.edu.learning.courseFormat.Lesson

class StepikLesson : Lesson() {
  var stepIds: List<Int> = listOf()
  var unitId = 0

  override val itemType: String
    get() = STEPIK
}
