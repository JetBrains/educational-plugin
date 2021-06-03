package com.jetbrains.edu.cpp.checker

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.project.Project
import com.jetbrains.edu.cpp.getCMakeProjectUniqueName
import com.jetbrains.edu.learning.checker.TheoryTaskChecker
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

class CppTheoryTaskChecker(task: TheoryTask, project: Project) : TheoryTaskChecker(task, project) {
  override fun getRunConfiguration(): RunnerAndConfigurationSettings? {
    val taskProjectName = "${getCMakeProjectUniqueName(task)}-run"
    return RunManager.getInstance(project).findConfigurationByName(taskProjectName)
  }
}