package com.jetbrains.edu.python.learning.newproject

import com.intellij.execution.ExecutionException
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.python.learning.installRequiredPackages
import com.jetbrains.edu.python.learning.messages.EduPythonBundle.message
import com.jetbrains.python.newProject.PyNewProjectSettings
import com.jetbrains.python.packaging.PyPackageManager
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PySdkToInstall
import com.jetbrains.python.sdk.associateWithModule
import com.jetbrains.python.sdk.createSdkByGenerateTask

open class PyCourseProjectGenerator(
  builder: EduCourseBuilder<PyNewProjectSettings>,
  course: Course
) : CourseProjectGenerator<PyNewProjectSettings>(builder, course) {

  override fun afterProjectGenerated(project: Project, projectSettings: PyNewProjectSettings) {
    super.afterProjectGenerated(project, projectSettings)
    var sdk = projectSettings.sdk
    if (sdk is PySdkToInstall) {
      ApplicationManager.getApplication().invokeAndWait {
        PyLanguageSettings.installSdk(sdk as PySdkToInstall)
      }
      createAndAddVirtualEnv(project, projectSettings)
      sdk = projectSettings.sdk
    }
    if (sdk != null && sdk.sdkType === PyFakeSdkType) {
      createAndAddVirtualEnv(project, projectSettings)
      sdk = projectSettings.sdk
    }
    sdk = updateSdkIfNeeded(project, sdk)
    SdkConfigurationUtil.setDirectoryProjectSdk(project, sdk)
    if (sdk == null) {
      return
    }
    installRequiredPackages(project, sdk)
  }

  private fun createAndAddVirtualEnv(project: Project, settings: PyNewProjectSettings) {
    val course = StudyTaskManager.getInstance(project).course ?: return
    val baseSdkPath = getBaseSdkPath(settings, course) ?: return
    val baseSdk = PyDetectedSdk(baseSdkPath)
    val virtualEnvPath = project.basePath + "/.idea/VirtualEnvironment"
    val sdk = createSdkByGenerateTask(object : Task.WithResult<String, ExecutionException>(
      project,
      message("creating.virtual.environment"),
      false
    ) {
      override fun compute(indicator: ProgressIndicator): String {
        indicator.isIndeterminate = true
        return PyPackageManager.getInstance(baseSdk).createVirtualEnv(virtualEnvPath, false)
      }
    }, allSdks, baseSdk, project.basePath, null)
    if (sdk == null) {
      LOG.warn("Failed to create virtual env in $virtualEnvPath")
      return
    }
    settings.sdk = sdk
    SdkConfigurationUtil.addSdk(sdk)
    sdk.associateWithModule(null, project.basePath)
  }

  companion object {
    private val LOG = Logger.getInstance(PyCourseProjectGenerator::class.java)

    private fun getBaseSdkPath(settings: PyNewProjectSettings, course: Course): String? {
      if (isUnitTestMode) {
        val sdk = settings.sdk
        return sdk?.homePath
      }
      return PyLanguageSettings.getBaseSdk(course)?.path
    }

    private fun updateSdkIfNeeded(project: Project, sdk: Sdk?): Sdk? {
      return PySdkSettingsHelper.firstAvailable().updateSdkIfNeeded(project, sdk)
    }

    private val allSdks: List<Sdk>
      get() {
        return PySdkSettingsHelper.firstAvailable().getAllSdks()
      }
  }
}
