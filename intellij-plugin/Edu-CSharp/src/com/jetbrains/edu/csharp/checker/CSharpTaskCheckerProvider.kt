package com.jetbrains.edu.csharp.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CodeExecutor
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

class CSharpTaskCheckerProvider : TaskCheckerProvider {
  override val codeExecutor: CodeExecutor
    get() = CSharpCodeExecutor()

  override fun getEduTaskChecker(task: EduTask, project: Project) = CSharpEduTaskChecker(task, envChecker, project)
}