package com.jetbrains.edu.python

import com.intellij.python.community.services.systemPython.SystemPythonProvider
import com.intellij.testFramework.ExtensionTestUtil.maskExtensions
import com.jetbrains.edu.learning.EduHeavyTestCase

/**
 * Disables python lookup in tests where it's not necessary.
 *
 * Temporary solution to avoid timeouts on some Windows TeamCity agents
 */
fun EduHeavyTestCase.disablePythonProviders() {
  maskExtensions(SystemPythonProvider.EP, emptyList(), testRootDisposable)
}