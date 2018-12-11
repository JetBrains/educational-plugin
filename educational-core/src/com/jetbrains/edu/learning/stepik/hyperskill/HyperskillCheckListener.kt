package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.projectView.ProgressUtil
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionView

class HyperskillCheckListener : CheckListener {
  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    val course = task.lesson.course
    if (course !is HyperskillCourse) return
    val (solved, total) = ProgressUtil.countProgress(course)
    if (solved == total) {
      TaskDescriptionView.getInstance(project).showBalloon("Congratulations! You finished this project. " +
                                       "Visit <a href=\"https://hyperskill.org/\">Hyperskill</a> to choose new project.", MessageType.INFO)
    }
  }
}