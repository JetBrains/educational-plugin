package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.createCourseFiles
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class MockHyperskillProjectManager: HyperskillProjectManager() {
  var project: Project? = null
  var module: Module? = null

  override fun newProject(course: HyperskillCourse) {
    project ?: error("Set up project explicitly in tests")
    module ?: error("Set up module explicitly in tests")
    course.createCourseFiles(project!!, module!!)
  }
}