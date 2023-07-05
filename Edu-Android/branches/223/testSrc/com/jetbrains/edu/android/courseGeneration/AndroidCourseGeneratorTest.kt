package com.jetbrains.edu.android.courseGeneration

import com.android.tools.idea.gradle.project.AndroidGradleProjectStartupActivity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.startup.StartupActivity

class AndroidCourseGeneratorTest : AndroidCourseGeneratorTestBase() {
  override fun disableUnnecessaryExtensions() {
    super.disableUnnecessaryExtensions()
    ApplicationManager.getApplication().extensionArea
      .getExtensionPoint(StartupActivity.POST_STARTUP_ACTIVITY)
      .unregisterExtensionInTest(AndroidGradleProjectStartupActivity::class.java)
  }
}