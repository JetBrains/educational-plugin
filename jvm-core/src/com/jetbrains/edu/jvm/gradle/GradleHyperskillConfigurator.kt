package com.jetbrains.edu.jvm.gradle

import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.gradle.checker.GradleTaskCheckerProvider
import com.jetbrains.edu.jvm.stepik.findCodeTaskFile
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator
import com.jetbrains.edu.learning.stepik.hyperskill.checker.HyperskillTaskCheckerProvider

abstract class GradleHyperskillConfigurator<T>(baseConfigurator: EduConfigurator<T>) : HyperskillConfigurator<T>(baseConfigurator) {
  override fun getCodeTaskFile(project: Project, task: Task): TaskFile? {
    val hyperskillProvider = taskCheckerProvider as? HyperskillTaskCheckerProvider
    val provider = hyperskillProvider?.baseProvider as? GradleTaskCheckerProvider ?: return super.getCodeTaskFile(project, task)
    return findCodeTaskFile(project, task, provider::mainClassForFile) ?: super.getCodeTaskFile(project, task)
  }
}