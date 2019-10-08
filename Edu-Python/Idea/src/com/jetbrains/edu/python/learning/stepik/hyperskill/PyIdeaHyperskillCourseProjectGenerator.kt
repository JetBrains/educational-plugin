package com.jetbrains.edu.python.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillSettings
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.python.learning.PyIdeaCourseProjectGenerator

class PyIdeaHyperskillCourseProjectGenerator(builder: PyIdeaHyperskillCourseBuilder, course: Course) :
  PyIdeaCourseProjectGenerator(builder, course) {

  override fun beforeProjectGenerated(): Boolean {
    assert(myCourse is HyperskillCourse)
    return myCourse.courseMode == CCUtils.COURSE_MODE || HyperskillConnector.getInstance().fillHyperskillCourse(myCourse as HyperskillCourse)
  }

  override fun loadSolutions(project: Project, course: Course) {
    if (course.isStudy && course is HyperskillCourse && HyperskillSettings.INSTANCE.account != null) {
      HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground()
    }
  }
}
