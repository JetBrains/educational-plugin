package com.jetbrains.edu.learning.statistics

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseParamsProcessor

class CoursePageExperimentParamProcessor : CourseParamsProcessor<CoursePageExperiment> {
  override fun findApplicableContext(params: Map<String, String>): CoursePageExperiment? = CoursePageExperiment.fromParams(params)

  override fun processCourseParams(project: Project, course: Course, context: CoursePageExperiment) {
    CoursePageExperimentManager.getInstance(project).experiment = context
  }
}
