package com.jetbrains.edu.python.learning.environment

import com.intellij.execution.ExecutionException
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.environment.InstallationResult
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironment
import com.jetbrains.edu.python.learning.installRequiredPackages
import com.jetbrains.edu.python.learning.messages.EduPythonBundle.message
import com.jetbrains.edu.python.learning.newproject.PySdkToCreateVirtualEnv
import com.jetbrains.python.configuration.PyConfigurableInterpreterList
import com.jetbrains.python.getOrThrow
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PySdkToInstall
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.PythonSdkUpdater
import com.jetbrains.python.sdk.createSdkByGenerateTask
import com.jetbrains.python.sdk.detectSystemWideSdks
import com.jetbrains.python.sdk.setAssociationToModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PyLanguageEnvironment(val sdk: Sdk, val languageLevel: LanguageLevel) : LanguageEnvironment {

  override suspend fun installIfNeeded(project: Project, course: Course): InstallationResult = withContext(Dispatchers.EDT) {
    installIfNeeded(project)
  }

  fun installIfNeeded(project: Project): InstallationResult {
    var sdk: Sdk? = sdk
    if (sdk is PySdkToInstall) {
      val selectedSdk = sdk

      @Suppress("UnstableApiUsage")
      val installedSdk = invokeAndWaitIfNeeded {
        selectedSdk.install(null) {
          detectSystemWideSdks(null, emptyList())
        }.getOrElse {
          LOG.warn(it)
          null
        }
      }
      if (installedSdk != null) {
        sdk = createAndAddVirtualEnv(project, installedSdk)
      }
    }
    if (sdk is PySdkToCreateVirtualEnv) {
      val homePath = sdk.homePath ?: error("Home path is not passed during fake python sdk creation")
      sdk = createAndAddVirtualEnv(project, PyDetectedSdk(homePath))
    }
    sdk = updateSdkIfNeeded(project, sdk)
    SdkConfigurationUtil.setDirectoryProjectSdk(project, sdk)
    if (sdk == null) {
      return InstallationResult.Error(message("error.failed.to.install.python.sdk"))
    }
    installRequiredPackages(project, sdk)

    return InstallationResult.Installed
  }

  private fun createAndAddVirtualEnv(project: Project, baseSdk: PyDetectedSdk): Sdk {
    val virtualEnvPath = project.basePath + "/.idea/VirtualEnvironment"
    val sdk = createSdkByGenerateTask(object : Task.WithResult<String, ExecutionException>(
      project,
      message("creating.virtual.environment"),
      false
    ) {
      @Throws(ExecutionException::class)
      override fun compute(indicator: ProgressIndicator): String {
        indicator.isIndeterminate = true
        return createVenv(baseSdk, virtualEnvPath)
      }
    }, PyConfigurableInterpreterList.getInstance(null).allPythonSdks, baseSdk, project.basePath, null)
    SdkConfigurationUtil.addSdk(sdk)
    val module = ModuleManager.getInstance(project).sortedModules.firstOrNull() ?: return sdk
    setAssociationToModule(sdk, module)

    return sdk
  }

  private fun setAssociationToModule(sdk: Sdk, module: Module) {
    runWithModalProgressBlocking(module.project, "") {
      sdk.setAssociationToModule(module)
    }
  }

  private fun createVenv(baseSdk: Sdk, virtualEnvPath: String): String {
    val pythonPath = baseSdk.homePath?.toNioPathOrNull() ?: error("Python home path is not found")
    val virtualEnvNioPath = virtualEnvPath.toNioPathOrNull() ?: error("Virtual env path is not found")
    return runBlockingMaybeCancellable {
      com.intellij.python.venv.createVenv(pythonPath, virtualEnvNioPath, false)
    }.getOrThrow().toString()
  }

  private fun updateSdkIfNeeded(project: Project, sdk: Sdk?): Sdk? {
    if (sdk !is PyDetectedSdk) {
      return sdk
    }
    val name = sdk.name
    val sdkHome = WriteAction.compute<VirtualFile, RuntimeException> { LocalFileSystem.getInstance().refreshAndFindFileByPath(name) }
    val newSdk = SdkConfigurationUtil.createAndAddSDK(sdkHome.path, PythonSdkType.getInstance())
    if (newSdk != null) {
      @Suppress("UnstableApiUsage")
      PythonSdkUpdater.updateOrShowError(newSdk, project, null)
    }
    return newSdk
  }

  companion object {
    private val LOG = logger<PyLanguageEnvironment>()
  }
}