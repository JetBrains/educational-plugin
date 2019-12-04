package com.jetbrains.edu.python.slow.checker

import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.EdtTestUtil
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.slow.checker.EduCheckerFixture
import com.jetbrains.python.newProject.PyNewProjectSettings
import com.jetbrains.python.sdk.PythonSdkType
import java.io.File

class PyCheckerFixture : EduCheckerFixture<PyNewProjectSettings>() {
  override val projectSettings: PyNewProjectSettings = PyNewProjectSettings()

  override fun setUp() {
    val sdkHomeDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(DEFAULT_SDK_LOCATION)) ?: error("Failed to find Python SDK")

    EdtTestUtil.runInEdtAndWait(ThrowableRunnable {
      val pythonSdk = SdkConfigurationUtil.setupSdk(emptyArray(), sdkHomeDir, PythonSdkType.getInstance(), true, null, MY_TEST_SDK_NAME)
                      ?: error("Failed to set up Python SDK")
      projectSettings.sdk = pythonSdk
      SdkConfigurationUtil.addSdk(pythonSdk)
    })
  }

  override fun tearDown() {
    val sdk = ProjectJdkTable.getInstance().findJdk(MY_TEST_SDK_NAME) ?: return
    SdkConfigurationUtil.removeSdk(sdk)
  }

  override fun getSkipTestReason(): String? {
    return if (DEFAULT_SDK_LOCATION == null) "no Python SDK location defined" else super.getSkipTestReason()
  }

  companion object {
    private val DEFAULT_SDK_LOCATION = System.getenv("PYTHON_SDK")
    private const val MY_TEST_SDK_NAME = "Test Python SDK"
  }
}
