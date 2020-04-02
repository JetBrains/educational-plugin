package com.jetbrains.edu.python.learning.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

class PyNewTaskCheckerProvider : TaskCheckerProvider {
  override val envChecker: EnvironmentChecker
    get() = PyEnvironmentChecker()

  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> =
    PyNewEduTaskChecker(task, envChecker, project)
}
