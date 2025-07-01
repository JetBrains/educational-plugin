package com.jetbrains.edu.learning.marketplace.lti

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.CourseSetListener
import com.jetbrains.edu.learning.courseFormat.Course

class LTICourseSetListener(private val project: Project) : CourseSetListener {
  override fun courseSet(course: Course) {
    // make sure service is loaded
    LTISettingsManager.getInstance(project)
  }
}