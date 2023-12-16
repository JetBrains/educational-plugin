package com.jetbrains.edu.learning

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseGeneration.ProjectOpener

class MockProjectOpener: ProjectOpener() {
  var project: Project? = null

  override fun newProject(course: Course): Boolean {
    assertProject()
    course.configurator?.beforeCourseStarted(course)
    course.createCourseFiles(project!!)
    return true
  }

  override fun focusOpenProject(coursePredicate: (Course) -> Boolean): Pair<Project, Course>? {
    assertProject()
    val course = project!!.course ?: return null
    return if (coursePredicate(course)) project!! to course else null
  }

  private fun assertProject() {
    project ?: error("Set up project explicitly in tests")
  }
}