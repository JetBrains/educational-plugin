package com.jetbrains.edu.jvm.stepik.hyperskill

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.jvm.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

open class HyperskillGradleCourseProjectGenerator(
  builder: GradleCourseBuilderBase,
  course: Course
) : GradleCourseProjectGenerator(builder, course) {

  override fun beforeProjectGenerated(): Boolean {
    assert(myCourse is HyperskillCourse)
    return myCourse.courseMode == CCUtils.COURSE_MODE || HyperskillConnector.getInstance().fillHyperskillCourse(myCourse as HyperskillCourse)
  }
}
