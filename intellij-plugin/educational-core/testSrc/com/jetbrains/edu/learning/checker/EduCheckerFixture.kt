package com.jetbrains.edu.learning.checker

import com.intellij.testFramework.fixtures.impl.BaseFixture
import com.jetbrains.edu.learning.newproject.EduProjectSettings

abstract class EduCheckerFixture<Settings : EduProjectSettings> : BaseFixture() {
  abstract val projectSettings: Settings
  open fun getSkipTestReason(): String? = null
}
