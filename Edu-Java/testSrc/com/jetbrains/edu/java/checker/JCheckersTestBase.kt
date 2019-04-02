package com.jetbrains.edu.java.checker

import com.jetbrains.edu.java.JCourseBuilder
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.EduCourseBuilder

abstract class JCheckersTestBase : JdkCheckerTestBase() {
  override val courseBuilder: EduCourseBuilder<JdkProjectSettings> = JCourseBuilder()
}
