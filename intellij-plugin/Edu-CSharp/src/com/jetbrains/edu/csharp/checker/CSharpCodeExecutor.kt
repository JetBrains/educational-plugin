package com.jetbrains.edu.csharp.checker

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.DefaultCodeExecutor
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class CSharpCodeExecutor : DefaultCodeExecutor() {
  override fun createRunConfiguration(project: Project, task: Task): RunnerAndConfigurationSettings? {
    val runManagerInstance = RunManager.getInstance(project)
    TODO("chipi chipi chapa chapa")
//    println("CONF: ${runManagerInstance.selectedConfiguration}")
    return runManagerInstance.selectedConfiguration
  }
}