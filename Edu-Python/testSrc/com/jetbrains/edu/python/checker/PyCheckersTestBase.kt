package com.jetbrains.edu.python.checker

import com.jetbrains.edu.learning.checker.CheckersTestBase
import com.jetbrains.edu.learning.checker.EduCheckerFixture
import com.jetbrains.python.newProject.PyNewProjectSettings

// This test runs only when PYTHON_SDK environment variable is defined and points to the valid python interpreter.
abstract class PyCheckersTestBase : CheckersTestBase<PyNewProjectSettings>() {
  override fun createCheckerFixture(): EduCheckerFixture<PyNewProjectSettings> = PyCheckerFixture()
}
