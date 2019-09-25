package com.jetbrains.edu.python.checker

import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.EdtTestUtil
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.learning.checker.CheckersTestBase
import com.jetbrains.python.newProject.PyNewProjectSettings
import com.jetbrains.python.sdk.PythonSdkType
import java.io.File

// This test runs only when PYTHON_SDK environment variable is defined and points to the valid python interpreter.
abstract class PyCheckersTestBase : CheckersTestBase<PyNewProjectSettings>() {
  override val projectSettings: PyNewProjectSettings = PyNewProjectSettings()

  override fun setUpEnvironment() {
    if (DEFAULT_SDK_LOCATION == null) {
      System.err.println("SKIP $name: no Python SDK location defined")
      return
    }
    val sdkHomeDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(DEFAULT_SDK_LOCATION))!!
    EdtTestUtil.runInEdtAndWait(ThrowableRunnable {
      projectSettings.sdk = SdkConfigurationUtil.setupSdk(
        emptyArray(), sdkHomeDir, PythonSdkType.getInstance(), true, null, MY_TEST_SDK_NAME)
      SdkConfigurationUtil.addSdk(projectSettings.sdk!!)
    })
  }

  override fun tearDownEnvironment() {
    val sdk = ProjectJdkTable.getInstance().findJdk(MY_TEST_SDK_NAME) ?: return
    SdkConfigurationUtil.removeSdk(sdk)
  }

  override fun runTest() {
    if (projectSettings.sdk == null) {
      System.err.println("SKIP $name: no Python SDK found")
      return
    }
    super.runTest()
  }

  companion object {
    private val DEFAULT_SDK_LOCATION = System.getenv("PYTHON_SDK")
    private const val MY_TEST_SDK_NAME = "Test Python SDK"
  }
}
