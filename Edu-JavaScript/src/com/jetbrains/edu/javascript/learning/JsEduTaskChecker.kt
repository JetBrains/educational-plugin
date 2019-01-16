package com.jetbrains.edu.javascript.learning

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask


class JsEduTaskChecker(task: EduTask, project: Project) : TaskChecker<EduTask>(task, project) {
  override fun check(indicator: ProgressIndicator): CheckResult {
    return CheckResult.NO_LOCAL_CHECK
  }
}

