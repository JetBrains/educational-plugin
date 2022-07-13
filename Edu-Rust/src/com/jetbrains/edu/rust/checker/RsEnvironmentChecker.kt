package com.jetbrains.edu.rust.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_RUST
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.rust.messages.EduRustBundle
import org.rust.cargo.project.model.cargoProjects
import org.rust.cargo.project.settings.rustSettings

class RsEnvironmentChecker : EnvironmentChecker() {
  override fun getEnvironmentError(project: Project, task: Task): CheckResult? {
    val toolchain = project.rustSettings.toolchain
    if (toolchain == null || !toolchain.looksLikeValidToolchain()) {
      return CheckResult(CheckStatus.Unchecked, EduRustBundle.message("error.no.toolchain.location", ENVIRONMENT_CONFIGURATION_LINK_RUST))
    }

    if (!project.cargoProjects.hasAtLeastOneValidProject) {
      return CheckResult(CheckStatus.Unchecked, EduRustBundle.message("error.no.cargo.project", ENVIRONMENT_CONFIGURATION_LINK_RUST))
    }

    return null
  }
}