package com.jetbrains.edu.learning.marketplace.certificate

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class CourseCertificateCheckListener : CheckListener {

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    if (!result.isSolved) return
    CourseCertificateManager.getInstance(project).notifyIfEarned(task.course)
  }
}
