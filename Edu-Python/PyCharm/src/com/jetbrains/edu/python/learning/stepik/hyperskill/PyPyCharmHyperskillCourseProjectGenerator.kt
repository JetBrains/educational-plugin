package com.jetbrains.edu.python.learning.stepik.hyperskill

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.python.learning.PyPyCharmCourseProjectGenerator

class PyPyCharmHyperskillCourseProjectGenerator(builder: PyPyCharmHyperskillCourseBuilder, course: Course) :
  PyPyCharmCourseProjectGenerator(builder, course) {

  override fun beforeProjectGenerated(): Boolean {
    assert(myCourse is HyperskillCourse)
    return myCourse.courseMode == CCUtils.COURSE_MODE || HyperskillConnector.getInstance().fillHyperskillCourse(myCourse as HyperskillCourse)
  }
}
