package com.jetbrains.edu.shell

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

class ShellTaskCheckerProvider : TaskCheckerProvider {
  // All Shell Script tasks are remote ones, so only remote checking will be performed
  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask>? = null
}