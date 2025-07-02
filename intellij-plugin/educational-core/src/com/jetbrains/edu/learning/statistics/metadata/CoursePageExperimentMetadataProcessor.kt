package com.jetbrains.edu.learning.statistics.metadata

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessor

class CoursePageExperimentMetadataProcessor : CourseMetadataProcessor<CoursePageExperiment> {
  override fun findApplicableMetadata(rawMetadata: Map<String, String>): CoursePageExperiment? = CoursePageExperiment.fromParams(rawMetadata)

  override fun processMetadata(project: Project, course: Course, metadata: CoursePageExperiment) {
    CoursePageExperimentManager.getInstance(project).experiment = metadata
  }
}
