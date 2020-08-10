package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.createCourseFiles
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class MockHyperskillProjectManager : HyperskillProjectManager() {
  var project: Project? = null

  override fun newProject(course: HyperskillCourse) {
    project ?: error("Set up project explicitly in tests")
    course.createCourseFiles(project!!, ModuleManager.getInstance(project!!).modules[0])
  }

  override fun focusOpenProject(coursePredicate: (Course) -> Boolean): Pair<Project, Course>? {
    val course = project!!.course ?: return null
    return if (coursePredicate(course)) project!! to course else null
  }
}