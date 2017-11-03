package com.jetbrains.edu.python.learning

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.PythonSdkUpdater

internal class IDEAPyDirectoryProjectGenerator(course: Course) : PyDirectoryProjectGenerator(course) {

  override fun getAllSdks(): List<Sdk> =
          ProjectJdkTable.getInstance().getSdksOfType(PythonSdkType.getInstance())


  override fun updateSdkIfNeeded(project: Project, sdk: Sdk?): Sdk? {
    return if (sdk is PyDetectedSdk) {
      val name = sdk.name
      val sdkHome = WriteAction.compute<VirtualFile, RuntimeException> { LocalFileSystem.getInstance().refreshAndFindFileByPath(name) }
      val newSdk = SdkConfigurationUtil.createAndAddSDK(sdkHome.path, PythonSdkType.getInstance())
      if (newSdk != null) {
        PythonSdkUpdater.updateOrShowError(newSdk, null, project, null)
        SdkConfigurationUtil.addSdk(newSdk)
      }
      newSdk
    } else sdk
  }
}
