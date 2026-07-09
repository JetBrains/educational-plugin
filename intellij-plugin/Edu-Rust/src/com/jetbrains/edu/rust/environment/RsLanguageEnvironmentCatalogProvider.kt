package com.jetbrains.edu.rust.environment

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.UserDataHolder
import com.jetbrains.edu.learning.DefaultSettingsUtils.findPath
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.flatMap
import com.jetbrains.edu.learning.newproject.environment.EnvironmentUiKind
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironmentCatalog
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironmentCatalogProvider
import com.jetbrains.edu.rust.messages.EduRustBundle
import org.rust.cargo.toolchain.RsLocalToolchain
import org.rust.cargo.toolchain.RsToolchainProvider
import org.rust.cargo.toolchain.flavors.RsToolchainFlavor
import org.rust.cargo.toolchain.tools.rustc
import java.nio.file.Path
import java.nio.file.Paths

class RsLanguageEnvironmentCatalogProvider : LanguageEnvironmentCatalogProvider<RsLanguageEnvironment> {
  override val uiKind: EnvironmentUiKind
    get() = EnvironmentUiKind.ComboBox

  override suspend fun default(): Result<RsLanguageEnvironment, String> {
    return findPath(DEFAULT_TOOLCHAIN_PROPERTY, "Rust toolchain").flatMap { toolchainPath ->
      val toolchain = RsLocalToolchain(Paths.get(toolchainPath))
      if (!toolchain.looksLikeValidToolchain()) {
        return@flatMap Err(EduRustBundle.message("error.toolchain.looks.invalid", toolchainPath))
      }
      Ok(RsLanguageEnvironment.Existing(toolchain))
    }
  }

  override suspend fun collectEnvironmentsForCourse(
    course: Course,
    context: UserDataHolder?
  ): Result<LanguageEnvironmentCatalog<RsLanguageEnvironment>, String> {
    val toolchainPaths = getAvailableToolchainsPaths()

    val toolchains = toolchainPaths
      .mapNotNull { RsToolchainProvider.getToolchain(it) }
      .filter { it.looksLikeValidToolchain() }

    val defaultProject = ProjectManager.getInstance().defaultProject
    val environments = toolchains.map {
      val version = runCatching {
        it.rustc().queryVersion(defaultProject)
      }.getOrElse { e ->
        LOG.warn("Failed to query rustc version", e)
        null
      }
      RsLanguageEnvironment.Existing(it, version)
    }

    return Ok(
      if (environments.isNotEmpty()) {
        LanguageEnvironmentCatalog(environments)
      }
      else {
        LanguageEnvironmentCatalog(RsLanguageEnvironment.Install)
      }
    )
  }

  private fun getAvailableToolchainsPaths(): List<Path> {
    return try {
      RsToolchainFlavor
        .getApplicableFlavors(null)
        .flatMap { it.suggestHomePaths(null) }
        .distinct()
    }
    catch (e: Exception) {
      LOG.warn("Failed to detect Rust toolchains", e)
      emptyList()
    }
  }

  companion object {
    private val LOG = logger<RsLanguageEnvironmentCatalogProvider>()
    private const val DEFAULT_TOOLCHAIN_PROPERTY = "project.rust.toolchain"
  }
}