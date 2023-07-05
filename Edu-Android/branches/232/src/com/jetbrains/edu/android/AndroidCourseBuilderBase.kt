package com.jetbrains.edu.android

import com.android.ide.common.gradle.Dependency
import com.android.ide.common.gradle.RichVersion
import com.android.tools.idea.gradle.repositories.RepositoryUrlManager
import com.android.tools.idea.sdk.AndroidSdks
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase

// BACKCOMPAT: 2022.3. Inline it
abstract class AndroidCourseBuilderBase : GradleCourseBuilderBase() {
  protected fun getLibraryVersion(groupId: String, artifactId: String, defaultVersion: String): String {
    val dependency = Dependency(groupId, artifactId, RichVersion.parse("+"))
    val sdkHandler = AndroidSdks.getInstance().tryToChooseSdkHandler()
    return RepositoryUrlManager.get().resolveDependencyRichVersion(dependency, null, sdkHandler) ?: defaultVersion
  }
}
