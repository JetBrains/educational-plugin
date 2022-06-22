package com.jetbrains.edu.jvm.gradle

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.MainFileProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator

abstract class GradleHyperskillConfigurator<T : Any>(baseConfigurator: EduConfigurator<T>) : HyperskillConfigurator<T>(baseConfigurator) {
  override fun getCodeTaskFile(project: Project, task: Task): TaskFile? {
    val language = task.course.languageById ?: return super.getCodeTaskFile(project, task)
    return runReadAction {
      for (file in task.taskFiles.values) {
        val virtualFile = file.getVirtualFile(project) ?: continue
        if (MainFileProvider.getMainClassName(project, virtualFile, language) != null) return@runReadAction file
      }
      null
    } ?: super.getCodeTaskFile(project, task)
  }
}
