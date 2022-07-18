package com.jetbrains.edu.php

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.php.messages.EduPhpBundle
import com.jetbrains.php.config.PhpProjectConfigurationFacade

class PhpEnvironmentChecker : EnvironmentChecker() {
  override fun getEnvironmentError(project: Project, task: Task): CheckResult? {
    val interpreter = PhpProjectConfigurationFacade.getInstance(project).interpreter
    return if (interpreter == null) {
      CheckResult(CheckStatus.Unchecked, EduPhpBundle.message("error.no.php.interpreter", EduNames.ENVIRONMENT_CONFIGURATION_LINK_PHP))
    }
    else null
  }
}