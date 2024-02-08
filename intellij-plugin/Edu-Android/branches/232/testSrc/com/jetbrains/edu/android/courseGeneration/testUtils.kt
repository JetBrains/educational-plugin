package com.jetbrains.edu.android.courseGeneration

import com.android.tools.idea.gradle.project.AndroidGradleProjectStartupActivity
import com.android.tools.idea.startup.GradleSpecificInitializer
import com.intellij.ide.ApplicationInitializedListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.startup.StartupActivity
import com.jetbrains.edu.android.courseGeneration.AndroidCourseGeneratorTest.Companion.unregisterExtensionInTest

// Disables some extensions provided by AS.
// They try to set up JAVA and Android JDK, or run Gradle import in tests where we don't need it.
// So let's unregister them. Otherwise, tests fail
// BACKCOMPAT" 2023.3. Inline
internal fun disableUnnecessaryExtensions(disposable: Disposable) {
  val extensionArea = ApplicationManager.getApplication().extensionArea

  @Suppress("UnstableApiUsage")
  extensionArea
    .getExtensionPoint<ApplicationInitializedListener>("com.intellij.applicationInitializedListener")
    .unregisterExtensionInTest(GradleSpecificInitializer::class.java, disposable)

  extensionArea
    .getExtensionPoint<StartupActivity>("com.intellij.postStartupActivity")
    // BACKCOMPAT: 2023.2. Use `unregisterExtensionInTest` when `AndroidGradleProjectStartupActivity` is migrated to `ProjectActivity` API
    .unregisterExtension(AndroidGradleProjectStartupActivity::class.java)
}