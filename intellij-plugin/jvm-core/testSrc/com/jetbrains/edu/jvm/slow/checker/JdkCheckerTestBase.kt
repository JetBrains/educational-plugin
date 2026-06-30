package com.jetbrains.edu.jvm.slow.checker

import com.jetbrains.edu.jvm.environment.JdkLanguageEnvironment
import com.jetbrains.edu.learning.checker.CheckersTestBase
import com.jetbrains.edu.learning.checker.EduCheckerFixture
import com.jetbrains.edu.learning.courseFormat.CheckResultDiff
import com.jetbrains.edu.learning.courseFormat.EduTestInfo
import org.hamcrest.Matcher

abstract class JdkCheckerTestBase : CheckersTestBase<JdkLanguageEnvironment>() {
  override fun createCheckerFixture(): EduCheckerFixture<JdkLanguageEnvironment> = JdkCheckerFixture()

  protected data class TestComparisonData(
    val messageMatcher: Matcher<String>,
    val diffMatcher: Matcher<CheckResultDiff?>,
    val executedTestsInfo: List<EduTestInfo> = emptyList()
  )
}
