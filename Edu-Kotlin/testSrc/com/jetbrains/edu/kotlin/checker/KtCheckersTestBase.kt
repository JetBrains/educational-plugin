package com.jetbrains.edu.kotlin.checker

import com.jetbrains.edu.kotlin.KtCourseBuilder
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.gradle.JdkProjectSettings

abstract class KtCheckersTestBase : JdkCheckerTestBase() {
  override val courseBuilder: EduCourseBuilder<JdkProjectSettings> = KtCourseBuilder()
}
