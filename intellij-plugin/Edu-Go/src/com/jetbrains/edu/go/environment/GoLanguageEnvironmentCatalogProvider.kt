package com.jetbrains.edu.go.environment

import com.goide.sdk.GoSdk
import com.goide.sdk.combobox.GoSdkList
import com.goide.util.GoUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.util.UserDataHolder
import com.intellij.util.text.VersionComparatorUtil
import com.jetbrains.edu.go.messages.EduGoBundle
import com.jetbrains.edu.learning.DefaultSettingsUtils.findPath
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.flatMap
import com.jetbrains.edu.learning.newproject.environment.EnvironmentUiKind
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironmentCatalog
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironmentCatalogProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException

class GoLanguageEnvironmentCatalogProvider : LanguageEnvironmentCatalogProvider<GoLanguageEnvironment> {
  override val uiKind: EnvironmentUiKind
    get() = EnvironmentUiKind.ComboBox

  override suspend fun default(): Result<GoLanguageEnvironment, String> {
    return findPath(DEFAULT_GO_SDK_PROPERTY, "Go sdk").flatMap { sdkPath ->
      val sdk = GoSdk.fromHomePath(sdkPath)
      when {
        sdk == GoSdk.NULL -> Err(EduGoBundle.message("error.no.sdk", sdkPath))
        !sdk.isValid -> Err(EduGoBundle.message("error.invalid.sdk.path", sdkPath))
        else -> Ok(GoLanguageEnvironment.Existing(sdk))
      }
    }
  }

  override suspend fun collectEnvironmentsForCourse(
    course: Course,
    context: UserDataHolder?
  ): Result<LanguageEnvironmentCatalog<GoLanguageEnvironment>, String> {
    val sdks = loadSdks()
    val environments = sdks.filter { it != GoSdk.NULL && it.isValid }.map { GoLanguageEnvironment.Existing(it) }

    if (environments.isNotEmpty()) {
      return Ok(LanguageEnvironmentCatalog(environments))
    }

    val latestVersion = getLatestGoSdkVersion()

    if (latestVersion == null) {
      return Err(EduGoBundle.message("error.failed.to.find.go.sdk.to.install"))
    }

    val installEnvironment = GoLanguageEnvironment.Install(latestVersion)

    return Ok(LanguageEnvironmentCatalog(installEnvironment))
  }

  private suspend fun loadSdks(): List<GoSdk> {
    return suspendCancellableCoroutine { continuation ->
      GoSdkList.getInstance().reloadSdks(null) { sdks ->
        continuation.resume(sdks) { th, _, _ ->
          LOG.warn("Loading of Go SDKs is cancelled", th)
        }
      }
    }
  }

  private fun getLatestGoSdkVersion(): String? {
    val os = GoUtil.systemOS()
    val arch = GoUtil.systemArch()
    val allVersions = try {
      GoDownloaderBridge
        .getAllAvailableGoSdkVersions(os, arch, EmptyProgressIndicator())
        .map { it.removePrefix("go") }
    }
    catch (e: IOException) {
      LOG.warn("Failed to get latest Go SDK version", e)
      null
    }

    return allVersions
      ?.sortedWith(VersionComparatorUtil.COMPARATOR)
      ?.lastOrNull()
  }

  companion object {
    private val LOG = logger<GoLanguageEnvironmentCatalogProvider>()
    private const val DEFAULT_GO_SDK_PROPERTY = "project.go.sdk"
  }
}
