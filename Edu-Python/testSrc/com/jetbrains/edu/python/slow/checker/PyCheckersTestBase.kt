package com.jetbrains.edu.python.slow.checker

import com.jetbrains.edu.python.slow.checker.PyCheckerFixture
import com.jetbrains.edu.slow.checker.CheckersTestBase
import com.jetbrains.edu.slow.checker.EduCheckerFixture
import com.jetbrains.python.newProject.PyNewProjectSettings

// This test runs only when PYTHON_SDK environment variable is defined and points to the valid python interpreter.
abstract class PyCheckersTestBase : CheckersTestBase<PyNewProjectSettings>() {
  override fun createCheckerFixture(): EduCheckerFixture<PyNewProjectSettings> = PyCheckerFixture()
}
