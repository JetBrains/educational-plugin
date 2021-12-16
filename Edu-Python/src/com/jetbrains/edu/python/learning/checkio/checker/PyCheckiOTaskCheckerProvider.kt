package com.jetbrains.edu.python.learning.checkio.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.checkio.checker.CheckiOTaskChecker
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.python.learning.checker.PyEnvironmentChecker
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiONames

class PyCheckiOTaskCheckerProvider : TaskCheckerProvider {
  override val envChecker: EnvironmentChecker
    get() = PyEnvironmentChecker()

  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> {
    return CheckiOTaskChecker(task, envChecker, project, PyCheckiOOAuthConnector, PyCheckiONames.PY_CHECKIO_INTERPRETER,
                              PyCheckiONames.PY_CHECKIO_TEST_FORM_TARGET_URL)
  }
}