package com.jetbrains.edu.go.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

class GoEduTaskChecker(project: Project, task: EduTask) : EduTaskCheckerBase(task, project) {
  override fun createTestConfigurations(): List<RunnerAndConfigurationSettings> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}
