package com.jetbrains.edu.slow.checkerTests.kotlin

import com.jetbrains.edu.slow.checkerTests.CourseValidationTestBase
import com.jetbrains.edu.slow.checkerTests.JDK_PROPERTY
import com.jetbrains.edu.slow.checkerTests.RequiredProperty
import com.jetbrains.edu.slow.checkerTests.ideaUltimate
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.TestFactory

@RequiredProperty(JDK_PROPERTY)
class KotlinBaseCheckCourseTest : CourseValidationTestBase("kotlin", ideaUltimate()) {

  @TestFactory
  fun `kotlin base test`(): List<DynamicNode> = doTest()
}
