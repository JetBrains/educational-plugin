package com.jetbrains.edu.learning.courseFormat

import com.jetbrains.edu.learning.EduNames

enum class CourseMode {
  STUDENT,
  EDUCATOR;

  override fun toString(): String = when (this) {
    STUDENT -> EduNames.STUDY
    EDUCATOR -> EduNames.EDUCATOR
  }
}