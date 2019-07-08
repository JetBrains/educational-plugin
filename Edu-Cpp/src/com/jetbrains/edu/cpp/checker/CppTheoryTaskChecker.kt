package com.jetbrains.edu.cpp.checker

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.TheoryTaskChecker
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

class CppTheoryTaskChecker(task: TheoryTask, project: Project) : TheoryTaskChecker(task, project) {
  override fun createTestConfiguration(): RunnerAndConfigurationSettings? {
    val taskProjectName = generateCMakeProjectUniqueName(task)
    return RunManager.getInstance(project).findConfigurationByName(taskProjectName)
  }
}