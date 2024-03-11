package com.jetbrains.edu.android.courseGeneration

import com.android.tools.idea.gradle.project.AndroidGradleProjectStartupActivity
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.startup.ProjectActivity

// Disables some extensions provided by AS.
// They try to set up JAVA and Android JDK, or run Gradle import in tests where we don't need it.
// So let's unregister them. Otherwise, tests fail
// BACKCOMPAT: 2023.3. Inline
internal fun disableUnnecessaryExtensions(disposable: Disposable) {
  ApplicationManager.getApplication().extensionArea
    .getExtensionPoint<ProjectActivity>("com.intellij.postStartupActivity")
    // Use `unregisterExtensionInTest` instead when we migrate our startup activities to `ProjectActivity` API
    .unregisterExtension(AndroidGradleProjectStartupActivity::class.java)
}
