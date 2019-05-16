package com.jetbrains.edu.jvm.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.jvm.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConnector.fillHyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillSettings
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillSolutionLoader
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

open class HyperskillGradleCourseProjectGenerator(builder: GradleCourseBuilderBase,
                                                  course: Course) : GradleCourseProjectGenerator(builder, course) {

  override fun beforeProjectGenerated(): Boolean {
    assert(myCourse is HyperskillCourse)
    return fillHyperskillCourse(myCourse as HyperskillCourse)
  }

  override fun afterProjectGenerated(project: Project, projectSettings: JdkProjectSettings) {
    super.afterProjectGenerated(project, projectSettings)
    HyperskillConnector.fillTopics(myCourse as HyperskillCourse, project)
  }

  override fun loadSolutions(project: Project, course: Course) {
    if (course.isStudy && course is HyperskillCourse && HyperskillSettings.INSTANCE.account != null) {
      HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground()
    }
  }
}
