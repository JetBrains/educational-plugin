package com.jetbrains.edu.python.learning.newproject

import com.intellij.execution.ExecutionException
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.python.learning.installRequiredPackages
import com.jetbrains.edu.python.learning.messages.EduPythonBundle.message
import com.jetbrains.edu.python.learning.newproject.PyLanguageSettings.Companion.installSdk
import com.jetbrains.edu.python.learning.newproject.PySdkSettingsHelper.Companion.firstAvailable
import com.jetbrains.python.packaging.PyPackageManager
import com.jetbrains.python.sdk.*

open class PyCourseProjectGenerator(
  builder: EduCourseBuilder<PyProjectSettings>,
  course: Course
) : CourseProjectGenerator<PyProjectSettings>(builder, course) {

  override fun afterProjectGenerated(project: Project, projectSettings: PyProjectSettings, onConfigurationFinished: () -> Unit) {
    var sdk = projectSettings.sdk
    if (sdk is PySdkToInstall) {
      val selectedSdk = sdk
      @Suppress("UnstableApiUsage")
      val installedSdk = invokeAndWaitIfNeeded {
        installSdk(selectedSdk)
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
    super.afterProjectGenerated(project, projectSettings, onConfigurationFinished)
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
        val packageManager = PyPackageManager.getInstance(baseSdk)
        return packageManager.createVirtualEnv(virtualEnvPath, false)
      }
    }, allSdks, baseSdk, project.basePath, null)
    if (sdk == null) {
      LOG.warn("Failed to create virtual env in $virtualEnvPath")
      return
    }
    settings.sdk = sdk
    SdkConfigurationUtil.addSdk(sdk)
    sdk.setAssociationToModule(project)
  }

  companion object {
    private val LOG = logger<PyCourseProjectGenerator>()

    private fun updateSdkIfNeeded(project: Project, sdk: Sdk?): Sdk? {
      val helper = firstAvailable()
      return helper.updateSdkIfNeeded(project, sdk)
    }

    private val allSdks: List<Sdk>
      get() {
        val helper = firstAvailable()
        return helper.getAllSdks()
      }
  }
}
