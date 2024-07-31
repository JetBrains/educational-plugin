package com.jetbrains.edu.learning.stepik.hyperskill.socialmedia

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task

object HyperskillSocialmediaUtils {

  fun askToPost(project: Project, solvedTask: Task, statusBeforeCheck: CheckStatus): Boolean {
    val course = project.course as? HyperskillCourse ?: return false
    if (!course.isStudy) return false
    if (statusBeforeCheck == CheckStatus.Solved) return false

    val projectLesson = course.getProjectLesson() ?: return false
    if (solvedTask.lesson != projectLesson) return false

    var allProjectTaskSolved = true
    projectLesson.visitTasks {
      allProjectTaskSolved = allProjectTaskSolved && it.status == CheckStatus.Solved
    }
    return allProjectTaskSolved
  }

}
