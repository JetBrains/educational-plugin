package com.jetbrains.edu.slow.checkerTests

import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.TestFactory

class PlainTextCourseValidationTest : CourseValidationTestBase("plain", ideInfo = ideaUltimate()) {

  @TestFactory
  fun `correct plain text course`(): List<DynamicNode> = doTest()
}
