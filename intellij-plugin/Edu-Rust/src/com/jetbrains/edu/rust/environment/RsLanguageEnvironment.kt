package com.jetbrains.edu.rust.environment

import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.newproject.environment.InstallationResult
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironment
import com.jetbrains.edu.rust.isSingleWorkspaceProject
import com.jetbrains.edu.rust.messages.EduRustBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.rust.cargo.CargoConstants
import org.rust.cargo.project.model.cargoProjects
import org.rust.cargo.project.settings.rustSettings
import org.rust.cargo.toolchain.RsToolchainBase
import org.rust.cargo.toolchain.impl.RustcVersion
import org.rust.cargo.toolchain.tools.Rustup
import org.rust.openapiext.pathAsPath

sealed class RsLanguageEnvironment : LanguageEnvironment {

  data object Install : RsLanguageEnvironment() {
    override suspend fun buildToolchain(project: Project): RsToolchainBase? {
      return Rustup.installRustup(project)?.toolchain
    }
  }

  data class Existing(val toolchain: RsToolchainBase, val version: RustcVersion? = null) : RsLanguageEnvironment() {
    override suspend fun buildToolchain(project: Project): RsToolchainBase = toolchain
  }

  data object NoOp : RsLanguageEnvironment() {
    override suspend fun buildToolchain(project: Project): RsToolchainBase? = null
    override suspend fun installIfNeeded(project: Project, course: Course): InstallationResult = InstallationResult.Installed
  }

  abstract suspend fun buildToolchain(project: Project): RsToolchainBase?

  override suspend fun installIfNeeded(project: Project, course: Course): InstallationResult {
    val toolchain = buildToolchain(project)
    if (toolchain == null) {
      return InstallationResult.Error(EduRustBundle.message("error.failed.to.install.rust.toolchain"))
    }

    withContext(Dispatchers.EDT) {
      project.rustSettings.modify {
        it.toolchain = toolchain
      }
    }

    attachCargoProjects(project, course)

    return InstallationResult.Installed
  }

  private suspend fun attachCargoProjects(project: Project, course: Course) {
    val manifestsPaths = buildList {
      if (!project.isSingleWorkspaceProject) {
        course.visitLessons {
          for (task in it.taskList) {
            val manifestFile = task.getDir(project.courseDir)?.findChild(CargoConstants.MANIFEST_FILE) ?: continue
            add(manifestFile.pathAsPath)
          }
        }
      }
    }

    project.cargoProjects.attachCargoProjects(manifestsPaths).await()
  }
}