package com.jetbrains.edu.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.python.sdk.PythonSdkType

internal class IDEAPyDirectoryProjectGenerator(course: Course) : PyDirectoryProjectGenerator(course) {

  override fun addSdk(project: Project, sdk: Sdk) {
    SdkConfigurationUtil.addSdk(sdk)
  }

  override fun getAllSdks(project: Project): List<Sdk> =
          ProjectJdkTable.getInstance().getSdksOfType(PythonSdkType.getInstance())
}
