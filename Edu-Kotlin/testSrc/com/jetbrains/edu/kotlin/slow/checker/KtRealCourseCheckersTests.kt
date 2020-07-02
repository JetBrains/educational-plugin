package com.jetbrains.edu.kotlin.slow.checker

import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.slow.checker.JdkCheckerFixture
import com.jetbrains.edu.learning.checker.EduCheckerFixture
import com.jetbrains.edu.slow.checker.RealCourseCheckersTestBase

abstract class KtRealCourseCheckersTestBase(courseId: Int) : RealCourseCheckersTestBase<JdkProjectSettings>(courseId) {
  override fun createCheckerFixture(): EduCheckerFixture<JdkProjectSettings> = JdkCheckerFixture()
}

class KotlinKoansTest : KtRealCourseCheckersTestBase(234424)
class AtomicKotlinTest : KtRealCourseCheckersTestBase(232600)
