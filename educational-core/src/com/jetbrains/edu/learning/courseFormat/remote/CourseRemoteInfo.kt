package com.jetbrains.edu.learning.courseFormat.remote

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Tag
import javax.swing.JPanel

interface CourseRemoteInfo {

  fun getTags(): List<Tag> = listOf()
  fun isCourseValid(course: Course): Boolean = true
  fun getAdditionalDescriptionPanel(project: Project): JPanel? = null
}

class LocalInfo : CourseRemoteInfo
