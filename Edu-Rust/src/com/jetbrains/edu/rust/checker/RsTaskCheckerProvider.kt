package com.jetbrains.edu.rust.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CodeExecutor
import com.jetbrains.edu.learning.checker.OutputTaskChecker
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask

class RsTaskCheckerProvider : TaskCheckerProvider {
  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> = RsEduTaskChecker(project, task)

  override fun getOutputTaskChecker(
    task: OutputTask,
    project: Project,
    codeExecutor: CodeExecutor
  ): OutputTaskChecker = RsOutputTaskChecker(project, task, codeExecutor)

  override fun getCodeExecutor(): CodeExecutor = RsCodeExecutor()
}
