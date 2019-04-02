package com.jetbrains.edu.kotlin.checker

import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.checker.JdkCheckerTestBase
import com.jetbrains.edu.kotlin.KtCourseBuilder
import com.jetbrains.edu.learning.EduCourseBuilder

abstract class KtCheckersTestBase : JdkCheckerTestBase() {
  override val courseBuilder: EduCourseBuilder<JdkProjectSettings> = KtCourseBuilder()
}
