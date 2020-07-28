package com.jetbrains.edu.python.slow.checker

import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.EdtTestUtil
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.learning.checker.EduCheckerFixture
import com.jetbrains.edu.python.learning.newproject.PyFakeSdkType
import com.jetbrains.python.newProject.PyNewProjectSettings
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor

class PyCheckerFixture : EduCheckerFixture<PyNewProjectSettings>() {
  override val projectSettings: PyNewProjectSettings = PyNewProjectSettings()

  override fun setUp() {
    EdtTestUtil.runInEdtAndWait(ThrowableRunnable {
      val versionString = PythonSdkFlavor.getApplicableFlavors(false)[0].getVersionString(DEFAULT_SDK_LOCATION)
      projectSettings.sdk = ProjectJdkImpl(versionString, PyFakeSdkType, DEFAULT_SDK_LOCATION, versionString)
      VfsRootAccess.allowRootAccess(testRootDisposable, DEFAULT_SDK_LOCATION)
    })
  }

  override fun tearDown() {
    for (pythonSdk in ProjectJdkTable.getInstance().getSdksOfType(PythonSdkType.getInstance())) {
      SdkConfigurationUtil.removeSdk(pythonSdk)
    }
  }

  override fun getSkipTestReason(): String? {
    return if (DEFAULT_SDK_LOCATION == null) {
      "No Python SDK location defined. Use `$PYTHON_SDK` environment variable to provide sdk location"
    } else {
      super.getSkipTestReason()
    }
  }

  companion object {
    private const val PYTHON_SDK = "PYTHON_SDK"
    private val DEFAULT_SDK_LOCATION = System.getenv(PYTHON_SDK)
  }
}
