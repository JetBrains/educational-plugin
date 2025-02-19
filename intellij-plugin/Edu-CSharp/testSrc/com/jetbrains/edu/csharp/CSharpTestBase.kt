package com.jetbrains.edu.csharp

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase

/**
 * A specialized base class related to testing C#-based projects.
 * There are several issues arising when using ordinary test classes:
 *  - a project cannot be initialized because the solution is not specified (the [CourseGenerationTestBase] class overrides project creation,
 *  so the issue does not arise)
 *  - code insight settings are broken after tests run ([CSharpTestBase] handles code insight settings)
 *  - etc.
 *
 * Starting from 2024.3 a set of base classes for C#-testing appear: `com.jetbrains.rider.test.BaseIntegrationTest` and others in the same
 * package. So this base class should probably be reimplemented to use them.
 */
abstract class CSharpTestBase : CourseGenerationTestBase<CSharpProjectSettings>() {

  override val defaultSettings: CSharpProjectSettings = CSharpProjectSettings()
  private lateinit var codeInsightSettingsState: CodeInsightSettings

  override fun setUp() {
    super.setUp()
    codeInsightSettingsState = CodeInsightSettings()
  }

  override fun tearDown() {
    try {
      // [com.intellij.testFramework.HeavyPlatformTestCase] checks code insight settings on `tearDown()`,
      // so we need to make sure that the proper ones are loaded
      codeInsightSettingsState.state?.let { CodeInsightSettings.getInstance().loadState(it) }
      // needs to be called before the project is disposed, as some of these events try
      // to access to Rider services and fail
      UIUtil.dispatchAllInvocationEvents()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }
}