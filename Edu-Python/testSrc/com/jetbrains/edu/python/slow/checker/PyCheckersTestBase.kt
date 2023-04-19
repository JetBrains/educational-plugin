package com.jetbrains.edu.python.slow.checker

import com.jetbrains.edu.learning.checker.CheckersTestBase
import com.jetbrains.edu.learning.checker.EduCheckerFixture
import com.jetbrains.edu.python.learning.newproject.PyProjectSettings

// This test runs only when PYTHON_SDK environment variable is defined and points to the valid python interpreter.
abstract class PyCheckersTestBase : CheckersTestBase<PyProjectSettings>() {
  override fun createCheckerFixture(): EduCheckerFixture<PyProjectSettings> = PyCheckerFixture()
}
