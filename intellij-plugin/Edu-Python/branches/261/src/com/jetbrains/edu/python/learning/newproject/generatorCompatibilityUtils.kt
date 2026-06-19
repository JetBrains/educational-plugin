package com.jetbrains.edu.python.learning.newproject

import com.intellij.execution.ExecutionException
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.invokeAndWaitIfNeeded
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
import com.intellij.python.venv.createVenv
import com.jetbrains.edu.python.learning.installRequiredPackages
import com.jetbrains.edu.python.learning.messages.EduPythonBundle.message
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGenerator.Companion.LOG
import com.jetbrains.python.configuration.PyConfigurableInterpreterList
import com.jetbrains.python.getOrThrow
import com.jetbrains.python.sdk.*

fun afterPyProjectGeneration(
  project: Project,
  projectSettings: PyProjectSettings,
  callback: () -> Unit
) {
  var sdk = projectSettings.sdk
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
      createAndAddVirtualEnv(project, projectSettings, installedSdk)
      sdk = projectSettings.sdk
    }
  }
  if (sdk is PySdkToCreateVirtualEnv) {
    val homePath = sdk.homePath ?: error("Home path is not passed during fake python sdk creation")
    createAndAddVirtualEnv(project, projectSettings, PyDetectedSdk(homePath))
    sdk = projectSettings.sdk
  }
  sdk = updateSdkIfNeeded(project, sdk)
  SdkConfigurationUtil.setDirectoryProjectSdk(project, sdk)
  if (sdk == null) {
    return
  }
  installRequiredPackages(project, sdk)
  callback()
}

private fun createAndAddVirtualEnv(project: Project, settings: PyProjectSettings, baseSdk: PyDetectedSdk) {
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
  if (sdk == null) {
    LOG.warn("Failed to create virtual env in $virtualEnvPath")
    return
  }
  settings.sdk = sdk
  SdkConfigurationUtil.addSdk(sdk)
  val module = ModuleManager.getInstance(project).sortedModules.firstOrNull() ?: return
  setAssociationToModule(sdk, module)
}

private fun setAssociationToModule(sdk: Sdk, module: Module) {
  runWithModalProgressBlocking(module.project, "") {
    sdk.setAssociationToModule(module)
  }
}

private fun createVenv(baseSdk: PySdkType, virtualEnvPath: String): String {
  val pythonPath = baseSdk.homePath?.toNioPathOrNull() ?: error("Python home path is not found")
  val virtualEnvNioPath = virtualEnvPath.toNioPathOrNull() ?: error("Virtual env path is not found")
  return runBlockingMaybeCancellable {
    createVenv(pythonPath, virtualEnvNioPath, false)
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
