package com.jetbrains.edu.learning.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask

interface TaskCheckerProvider {
  val codeExecutor: CodeExecutor
    get() = DefaultCodeExecutor()

  val envChecker: EnvironmentChecker
    get() = EnvironmentChecker()

  fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask>?

  // Should not be overloaded by anyone
  fun getTaskChecker(task: Task, project: Project): TaskChecker<*>? {
    return when (task) {
      is TheoryTask, is UnsupportedTask -> null
      is EduTask -> getEduTaskChecker(task, project)
      is OutputTask -> OutputTaskChecker(task, envChecker, project, codeExecutor)
      is ChoiceTask -> if (task.canCheckLocally) ChoiceTaskChecker(task, project) else null
      is IdeTask -> IdeTaskChecker(task, project)
      else -> throw IllegalStateException("Unknown task type: " + task.itemType)
    }
  }
}