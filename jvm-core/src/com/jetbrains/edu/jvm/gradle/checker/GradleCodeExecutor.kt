package com.jetbrains.edu.jvm.gradle.checker

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CodeExecutor
import com.jetbrains.edu.learning.checker.DefaultCodeExecutor
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class GradleCodeExecutor(private val mainClassForFile: (Project, VirtualFile) -> String?) : CodeExecutor {
  override fun execute(project: Project, task: Task, indicator: ProgressIndicator, input: String?): Result<String, CheckResult> =
    when (task) {
      // TODO https://youtrack.jetbrains.com/issue/EDU-3272
      is CodeforcesTask -> DefaultCodeExecutor().execute(project, task, indicator, input)
      else -> runGradleRunTask(project, task, indicator, mainClassForFile)
    }
}