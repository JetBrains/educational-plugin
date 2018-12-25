package com.jetbrains.edu.java.checker

import com.jetbrains.edu.java.JCourseBuilder
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.gradle.JdkProjectSettings

abstract class JCheckersTestBase : JdkCheckerTestBase() {
  override val courseBuilder: EduCourseBuilder<JdkProjectSettings> = JCourseBuilder()
}
