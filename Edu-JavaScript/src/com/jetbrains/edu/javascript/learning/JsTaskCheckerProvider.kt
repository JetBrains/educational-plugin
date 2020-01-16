package com.jetbrains.edu.javascript.learning

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

open class JsTaskCheckerProvider : TaskCheckerProvider {
  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> {
    return JsTaskChecker(task, project)
  }
}