package com.jetbrains.edu.android.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.gradle.checker.GradleEduTaskChecker
import com.jetbrains.edu.jvm.gradle.checker.GradleTaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

class AndroidTaskCheckerProvider : GradleTaskCheckerProvider() {
  override fun getEduTaskChecker(task: EduTask, project: Project): GradleEduTaskChecker = AndroidChecker(task, envChecker, project)
}
