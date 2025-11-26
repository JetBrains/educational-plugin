package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI

import com.intellij.openapi.Disposable
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProviderFactory
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import kotlinx.coroutines.CoroutineScope
import javax.swing.Icon

class HyperskillInstallPluginPlatformProviderFactory : CoursesPlatformProviderFactory {
  override fun getProviders(): List<CoursesPlatformProvider> = listOf(HyperskillInstallPluginPlatformProvider())
}

class HyperskillInstallPluginPlatformProvider : CoursesPlatformProvider() {
  override val name: String = HYPERSKILL

  override val icon: Icon get() = EducationalCoreIcons.Platform.Tab.JetBrainsAcademyTab

  override fun createPanel(
    scope: CoroutineScope,
    disposable: Disposable
  ): CoursesPanel {
    return HyperskillInstallPluginCoursesPanel(this, scope, disposable)
  }

  override suspend fun doLoadCourses(): List<CoursesGroup> = emptyList()
}