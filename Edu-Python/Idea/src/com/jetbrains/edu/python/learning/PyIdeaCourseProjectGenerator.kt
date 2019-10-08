package com.jetbrains.edu.python.learning

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGeneratorBase
import com.jetbrains.python.newProject.PyNewProjectSettings
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.PythonSdkUpdater

open class PyIdeaCourseProjectGenerator(
  builder: EduCourseBuilder<PyNewProjectSettings>,
  course: Course
) : PyCourseProjectGeneratorBase(builder, course) {

  override fun updateSdkIfNeeded(project: Project, sdk: Sdk?): Sdk? {
    if (sdk !is PyDetectedSdk) {
      return sdk
    }
    val name = sdk.name
    val sdkHome = WriteAction.compute<VirtualFile, RuntimeException> { LocalFileSystem.getInstance().refreshAndFindFileByPath(name) }
    val newSdk = SdkConfigurationUtil.createAndAddSDK(sdkHome.path, PythonSdkType.getInstance())
    if (newSdk != null) {
      PythonSdkUpdater.updateOrShowError(newSdk, null, project, null)
      SdkConfigurationUtil.addSdk(newSdk)
    }
    return newSdk
  }

  override fun getAllSdks(): List<Sdk> {
    return ProjectJdkTable.getInstance().getSdksOfType(PythonSdkType.getInstance())
  }
}
