package com.jetbrains.edu.python.learning.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

class PyTaskCheckerProvider : TaskCheckerProvider {
  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> = PyTaskChecker(task, project)
}