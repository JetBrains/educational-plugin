package com.jetbrains.edu.learning.framework

import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse

val FrameworkLesson.propagateFilesOnNavigation: Boolean get() {
  val thisCourse = course
  return !isTemplateBased || thisCourse is HyperskillCourse && !thisCourse.isTemplateBased
}