package com.jetbrains.edu.go.environment

import com.goide.project.GoModuleSettings
import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.goide.sdk.download.GoDownloadingSdk
import com.goide.vgo.configuration.VgoProjectSettings
import com.intellij.openapi.application.EDT
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.util.SystemProperties
import com.jetbrains.edu.go.messages.EduGoBundle
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.environment.InstallationResult
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.absolutePathString

sealed class GoLanguageEnvironment : LanguageEnvironment {

  data class Install(val version: String) : GoLanguageEnvironment() {
    override fun buildSdk(project: Project): GoSdk {
      return GoDownloadingSdk(version, defaultDownloadPath().absolutePathString())
    }
  }

  data class Existing(val sdk: GoSdk) : GoLanguageEnvironment() {
    override fun buildSdk(project: Project): GoSdk = sdk
  }

  data object NoOp : GoLanguageEnvironment() {
    override fun buildSdk(project: Project): GoSdk = GoSdk.NULL
    override suspend fun installIfNeeded(project: Project, course: Course): InstallationResult = InstallationResult.Installed
  }

  protected abstract fun buildSdk(project: Project): GoSdk

  override suspend fun installIfNeeded(project: Project, course: Course): InstallationResult {
    val sdk = buildSdk(project)
    if (sdk == GoSdk.NULL || !sdk.isValid) {
      return InstallationResult.Error(EduGoBundle.message("error.failed.to.install.go.sdk"))
    }

    applyToProject(project, sdk)
    return InstallationResult.Installed
  }

  private suspend fun applyToProject(project: Project, sdk: GoSdk) {
    withContext(Dispatchers.EDT) {
      GoSdkService.getInstance(project).setSdk(sdk)
      val module = ModuleManager.getInstance(project).modules.singleOrNull()
      if (module != null) {
        // Enable Go support immediately instead of waiting for GoPluginInitialConfigurator.
        GoModuleSettings.getInstance(module).isGoSupportEnabled = true
      }
      VgoProjectSettings.getInstance(project).isIntegrationEnabled = true
    }
  }

  companion object {
    // This is the default download path used by the Go plugin,
    // see com.goide.sdk.download.GoDownloadSdkAction.GoDownloadSdkDialog#getDefaultTargetPath
    private fun defaultDownloadPath(): Path = Path.of(SystemProperties.getUserHome(), "sdk")
  }
}
