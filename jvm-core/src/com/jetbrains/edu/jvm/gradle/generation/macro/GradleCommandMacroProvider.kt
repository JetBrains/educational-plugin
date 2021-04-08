package com.jetbrains.edu.jvm.gradle.generation.macro

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.jvm.gradle.GradleConfiguratorBase
import com.jetbrains.edu.jvm.gradle.checker.getGradleProjectName
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseGeneration.macro.EduMacro
import com.jetbrains.edu.learning.courseGeneration.macro.EduMacroProvider
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.learning.isTaskRunConfigurationFile

class GradleCommandMacroProvider : EduMacroProvider {

  override fun provideMacro(project: Project, file: VirtualFile): EduMacro? {
    return if (file.isTaskRunConfigurationFile(project) && project.course?.configurator is GradleConfiguratorBase) {
      val task = file.getContainingTask(project) ?: error("Failed to find task for `$file` file")
      val gradleProjectName = getGradleProjectName(task)
      EduMacro(TASK_GRADLE_PROJECT_NAME, gradleProjectName)
    } else {
      null
    }
  }

  companion object {
    private const val TASK_GRADLE_PROJECT_NAME = "TASK_GRADLE_PROJECT"
  }
}