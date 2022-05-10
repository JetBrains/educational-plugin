package com.jetbrains.edu.learning.stepik.course

import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.stepik.StepikNames

class StepikLesson : Lesson() {
  var steps: List<Int> = listOf()
  var unitId = 0

  override val itemType: String
    get() = StepikNames.STEPIK_TYPE
}
