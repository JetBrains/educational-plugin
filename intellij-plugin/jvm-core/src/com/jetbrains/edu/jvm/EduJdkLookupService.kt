package com.jetbrains.edu.jvm

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.runAndLogException
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkDownloadUtil
import com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkItem
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTracker
import com.intellij.util.cancelOnDispose
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Service(Service.Level.APP)
class EduJdkLookupService(private val scope: CoroutineScope) {

  /**
   * The context for the callback has the modality [modalityState], so the call to `withContext(Dispatchers.EDT)` inside
   * the callback will have this modality.
   */
  fun findSuitableJdk(
    courseSdkVersion: ParsedJavaVersion,
    sdkModel: ProjectSdksModel,
    modalityState: ModalityState,
    disposable: Disposable,
    @RequiresEdt callback: suspend (Sdk?) -> Unit
  ) {
    scope.launch(Dispatchers.IO + modalityState.asContextElement()) {
      val sdk = doFindSuitableJdk(courseSdkVersion, sdkModel)
      callback(sdk)
    }.cancelOnDispose(disposable)
  }

  fun downloadJdkIfNeeded(jdk: Sdk) {
    if (!SdkDownloadTracker.getInstance().isDownloading(jdk)) return

    scope.launch(Dispatchers.IO) {
      JdkDownloadUtil.downloadSdk(jdk)
    }
  }

  private suspend fun doFindSuitableJdk(courseSdkVersion: ParsedJavaVersion, sdkModel: ProjectSdksModel): Sdk? {
    val existingSdk = sdkModel.findExistingSdk(courseSdkVersion)
    if (existingSdk != null) return existingSdk
    val requiredJdkMajorVersion = (courseSdkVersion as? JavaVersionParseSuccess)?.javaSdkVersion?.maxLanguageLevel?.feature()
    return sdkModel.createDownloadableSdk(requiredJdkMajorVersion)
  }

  private suspend fun ProjectSdksModel.findExistingSdk(courseSdkVersion: ParsedJavaVersion): Sdk? {
    val (homePath, _) = JavaSdk.getInstance().collectSdkEntries(null)
                          .asSequence()
                          .map { e ->
                            val javaVersion = ParsedJavaVersion.fromJavaSdkVersionString(e.versionString)
                            e.homePath() to javaVersion
                          }
                          .filter { (homePath, javaVersion) ->
                            ExternalSystemJdkUtil.isValidJdk(homePath) && when {
                              // if the SDK doesn't specify the version, it does not fit
                              javaVersion !is JavaVersionParseSuccess -> false
                              // if the course does not specify the version, all SDKs fit
                              courseSdkVersion !is JavaVersionParseSuccess -> true
                              // otherwise, the version of the SDK must be at least the version required by the course
                              else -> javaVersion isAtLeast courseSdkVersion
                            }
                          }
                          .map { (homePath, javaVersion) -> homePath to javaVersion as JavaVersionParseSuccess }
                          .maxByOrNull { (_, javaVersion) -> javaVersion.javaSdkVersion }
                        ?: return null

    val existingSdk = lookupJdkByPath(ProjectManager.getInstance().defaultProject, homePath)

    // [lookupJdkByPath()] might add a new entry in the sdkModel of the default project.
    // We should sync the sdkModel of the jdkComboBox with sdkModel of the default project.
    withContext(Dispatchers.EDT) {
      syncSdks()
    }

    return existingSdk
  }

  private suspend fun ProjectSdksModel.createDownloadableSdk(jdkMajorVersion: Int?): Sdk? {
    val project = ProjectManager.getInstance().defaultProject

    val (jdkItem, jdkHome) = JdkDownloadUtil.pickJdkItemAndPath(project) { item ->
      !item.isPreview && item.matchesJdkMajorVersion(jdkMajorVersion)
    } ?: return null

    val task = JdkDownloadUtil.createDownloadTask(project, jdkItem, jdkHome)
    if (task == null) {
      LOG.warn("Failed to create a download task for JDK item=$jdkItem, home=$jdkHome")
      return null
    }

    return LOG.runAndLogException {
      writeAction {
        createIncompleteSdk(JavaSdk.getInstance(), task, null)
      }
    }
  }

  private fun JdkItem.matchesJdkMajorVersion(jdkMajorVersion: Int?): Boolean = jdkMajorVersion == null || this.jdkMajorVersion == jdkMajorVersion

  companion object {
    private val LOG = logger<EduJdkLookupService>()
    fun getInstance(): EduJdkLookupService = service()
  }
}