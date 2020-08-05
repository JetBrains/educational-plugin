package com.jetbrains.edu.kotlin.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.gradle.checker.GradleTaskCheckerProvider
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

class KtTaskCheckerProvider : GradleTaskCheckerProvider() {
  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> {
    return KtNewGradleTaskChecker(task, envChecker, project)
  }
}
