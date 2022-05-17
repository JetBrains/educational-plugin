@file:JvmName("OpenProjectUtils")

package com.jetbrains.edu.learning.newproject

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import java.nio.file.Path

fun openNewProject(location: Path, callback: (Module) -> Unit): Project? {
  val task = OpenProjectTask(
    forceOpenInNewFrame = true,
    projectToClose = null,
    isNewProject = true,
    runConfigurators = true,
    isProjectCreatedWithWizard = true,
    preparedToOpen = callback
  )

  return ProjectManagerEx.getInstanceEx().openProject(location, task)
}
