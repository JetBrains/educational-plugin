package com.jetbrains.edu.learning.checkio

import com.jetbrains.edu.learning.CoursesProvider
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.checkio.utils.CheckiONames.JS_CHECKIO
import com.jetbrains.edu.learning.checkio.utils.CheckiONames.PY_CHECKIO
import com.jetbrains.edu.learning.courseFormat.Course

class CheckiOCoursesProvider : CoursesProvider {
  override fun loadCourses(): List<Course> {
    return if (EduUtils.isAndroidStudio()) {
      emptyList()
    } else {
      listOf(
        CheckiOCourse(PY_CHECKIO, "${EduNames.PYTHON} ${EduNames.PYTHON_3_VERSION}"),
        CheckiOCourse(JS_CHECKIO, EduNames.JAVASCRIPT)
      )
    }
  }
}
