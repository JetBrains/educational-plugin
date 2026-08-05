package com.jetbrains.edu.slow.checkerTests

import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.TestFactory

private const val PYTHON_INTERPRETER_PROPERTY = "project.python.interpreter"

@RequiredProperty(PYTHON_INTERPRETER_PROPERTY)
class PythonBaseCheckCourseTest : CourseValidationTestBase("python", pyCharm()) {

  @TestFactory
  fun `python base test`(): List<DynamicNode> = doTest()
}