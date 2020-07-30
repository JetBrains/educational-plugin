package com.jetbrains.edu.rust.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CodeExecutor
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.rust.messages.EduRustBundle
import org.rust.cargo.project.model.cargoProjects
import org.rust.cargo.project.settings.rustSettings

class RsTaskCheckerProvider : TaskCheckerProvider {
  override val envChecker: EnvironmentChecker
    get() = object: EnvironmentChecker() {
      override fun checkEnvironment(project: Project, task: Task): String? {
        val toolchain = project.rustSettings.toolchain
        if (toolchain == null || !toolchain.looksLikeValidToolchain()) {
          return EduRustBundle.message("error.no.toolchain.location")
        }

        if (!project.cargoProjects.hasAtLeastOneValidProject) {
          return EduRustBundle.message("error.no.cargo.project")
        }

        return null
      }
    }

  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> = RsEduTaskChecker(project, envChecker, task)

  override fun getCodeExecutor(): CodeExecutor = RsCodeExecutor()
}
