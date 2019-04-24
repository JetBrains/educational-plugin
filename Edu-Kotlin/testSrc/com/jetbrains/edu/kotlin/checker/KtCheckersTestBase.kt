package com.jetbrains.edu.kotlin.checker

import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.checker.JdkCheckerTestBase
import com.jetbrains.edu.kotlin.KtCourseBuilder
import com.jetbrains.edu.learning.EduCourseBuilder
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher

abstract class KtCheckersTestBase : JdkCheckerTestBase() {
  override val courseBuilder: EduCourseBuilder<JdkProjectSettings> = KtCourseBuilder()

  protected inline fun <reified T> nullValue(): Matcher<T> = CoreMatchers.nullValue(T::class.java)
}
