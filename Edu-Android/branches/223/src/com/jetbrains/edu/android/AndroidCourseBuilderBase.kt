package com.jetbrains.edu.android

import com.android.ide.common.repository.GradleCoordinate
import com.android.tools.idea.gradle.repositories.RepositoryUrlManager
import com.android.tools.idea.sdk.AndroidSdks
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase

abstract class AndroidCourseBuilderBase : GradleCourseBuilderBase() {
  protected fun getLibraryVersion(groupId: String, artifactId: String, defaultVersion: String): String {
    val gradleCoordinate = GradleCoordinate(groupId, artifactId, "+")
    val sdkHandler = AndroidSdks.getInstance().tryToChooseSdkHandler()
    return RepositoryUrlManager.get().resolveDynamicCoordinateVersion(gradleCoordinate, null, sdkHandler) ?: defaultVersion
  }
}
