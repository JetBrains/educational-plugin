package com.jetbrains.edu.slow.checkerTests.scala

import com.jetbrains.edu.slow.checkerTests.CourseValidationTestBase
import com.jetbrains.edu.slow.checkerTests.JDK_PROPERTY
import com.jetbrains.edu.slow.checkerTests.RequiredPlugin
import com.jetbrains.edu.slow.checkerTests.RequiredProperty
import com.jetbrains.edu.slow.checkerTests.SCALA_PLUGIN_ID
import com.jetbrains.edu.slow.checkerTests.ideaUltimate
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.TestFactory

@RequiredProperty(JDK_PROPERTY)
@RequiredPlugin(SCALA_PLUGIN_ID)
class ScalaGradleCourseCheckerTest : CourseValidationTestBase("scala", ideaUltimate()) {

  @TestFactory
  fun `scala positive test`(): List<DynamicNode> = doTest()
}
