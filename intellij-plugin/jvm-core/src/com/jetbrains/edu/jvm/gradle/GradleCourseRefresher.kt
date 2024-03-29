package com.jetbrains.edu.jvm.gradle

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.RefreshCause

interface GradleCourseRefresher {
  fun isAvailable(): Boolean
  fun refresh(project: Project, cause: RefreshCause)

  companion object {
    val EP_NAME: ExtensionPointName<GradleCourseRefresher> = ExtensionPointName.create("Educational.gradleRefresher")

    fun firstAvailable(): GradleCourseRefresher? = EP_NAME.extensionList.firstOrNull(GradleCourseRefresher::isAvailable)
  }
}
