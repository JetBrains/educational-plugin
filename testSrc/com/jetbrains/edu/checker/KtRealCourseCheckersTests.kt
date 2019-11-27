package com.jetbrains.edu.checker

import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.checker.JdkCheckerFixture
import com.jetbrains.edu.kotlin.twitter.KtTwitterSettings
import com.jetbrains.edu.learning.checker.EduCheckerFixture

abstract class KtRealCourseCheckersTestBase(courseId: Int) : RealCourseCheckersTestBase<JdkProjectSettings>(courseId) {

  override fun createCheckerFixture(): EduCheckerFixture<JdkProjectSettings> = JdkCheckerFixture()

  override fun `test course`() {
    val settings = KtTwitterSettings.getInstance(myProject)
    val oldValue = settings.askToTweet()
    try {
      settings.setAskToTweet(false)
      super.`test course`()
    } finally {
      settings.setAskToTweet(oldValue)
    }
  }
}

class KotlinKoansTest : KtRealCourseCheckersTestBase(234424)
class AtomicKotlinTest : KtRealCourseCheckersTestBase(232600)
