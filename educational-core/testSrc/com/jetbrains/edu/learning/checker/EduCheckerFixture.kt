package com.jetbrains.edu.learning.checker

import com.intellij.testFramework.fixtures.impl.BaseFixture

abstract class EduCheckerFixture<Settings> : BaseFixture() {
  abstract val projectSettings: Settings
  open fun getSkipTestReason(): String? = null
}
