package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.createCourseFiles
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class MockHyperskillProjectManager: HyperskillProjectManager() {
  var project: Project? = null

  override fun newProject(course: HyperskillCourse) {
    project ?: error("Set up project explicitly in tests")
    course.createCourseFiles(project!!, ModuleManager.getInstance(project!!).modules[0])
  }
}