package com.jetbrains.edu.slow.checker

import com.intellij.testFramework.fixtures.impl.BaseFixture

abstract class EduCheckerFixture<Settings> : BaseFixture() {
  abstract val projectSettings: Settings
  open fun getSkipTestReason(): String? = null
}
