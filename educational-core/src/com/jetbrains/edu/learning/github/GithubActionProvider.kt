package com.jetbrains.edu.learning.github

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.TaskCustomActionProvider
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class GithubActionProvider : TaskCustomActionProvider {
  override fun getAction(project: Project): AnAction {
    return ShareProjectAction(project)
  }

  override fun isAvailable(project: Project): Boolean {
    val course = StudyTaskManager.getInstance(project).course
    return course is HyperskillCourse
  }
}