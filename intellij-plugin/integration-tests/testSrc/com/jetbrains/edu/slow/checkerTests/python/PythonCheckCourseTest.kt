package com.jetbrains.edu.slow.checkerTests.python

import com.jetbrains.edu.slow.checkerTests.CourseValidationTestBase
import com.jetbrains.edu.slow.checkerTests.PYTHON_INTERPRETER_PROPERTY
import com.jetbrains.edu.slow.checkerTests.RequiredProperty
import com.jetbrains.edu.slow.checkerTests.pyCharm
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.TestFactory

@RequiredProperty(PYTHON_INTERPRETER_PROPERTY)
class PythonBaseCheckCourseTest : CourseValidationTestBase("python", pyCharm()) {

  @TestFactory
  fun `python base test`(): List<DynamicNode> = doTest()
}
