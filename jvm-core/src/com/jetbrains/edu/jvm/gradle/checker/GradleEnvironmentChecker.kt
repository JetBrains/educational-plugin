package com.jetbrains.edu.jvm.gradle.checker

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.jetbrains.edu.jvm.messages.EduJVMBundle
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreErrorBundle
import org.jetbrains.plugins.gradle.settings.GradleSettings

class GradleEnvironmentChecker : EnvironmentChecker() {
  override fun checkEnvironment(project: Project, task: Task): String? {
    val sdk = ProjectRootManager.getInstance(project).projectSdk
    if (sdk == null) return EduCoreErrorBundle.message("no.sdk")

    val unableToCreateConfigurationError = EduCoreErrorBundle.message("unable.to.create.configuration")
    val taskDir = task.getDir(project.courseDir) ?: return unableToCreateConfigurationError
    val module = ModuleUtil.findModuleForFile(taskDir, project) ?: return unableToCreateConfigurationError

    val gradleNotImportedError = EduJVMBundle.message("error.gradle.not.imported")
    val path = ExternalSystemApiUtil.getExternalRootProjectPath(module) ?: return gradleNotImportedError
    return if (GradleSettings.getInstance(project).getLinkedProjectSettings(path) == null) gradleNotImportedError else null
  }
}