package com.jetbrains.edu.jvm.slow.checker

import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.checker.CheckersTestBase
import com.jetbrains.edu.learning.checker.EduCheckerFixture
import com.jetbrains.edu.learning.courseFormat.CheckResultDiff
import com.jetbrains.edu.learning.courseFormat.EduTestInfo
import org.hamcrest.Matcher

abstract class JdkCheckerTestBase : CheckersTestBase<JdkProjectSettings>() {
  override fun createCheckerFixture(): EduCheckerFixture<JdkProjectSettings> = JdkCheckerFixture()

  protected data class TestComparisonData(
    val messageMatcher: Matcher<String>,
    val diffMatcher: Matcher<CheckResultDiff?>,
    val executedTestsInfo: List<EduTestInfo> = emptyList()
  )
}
