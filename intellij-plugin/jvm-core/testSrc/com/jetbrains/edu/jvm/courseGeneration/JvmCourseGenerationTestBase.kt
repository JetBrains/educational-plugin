package com.jetbrains.edu.jvm.courseGeneration

import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl
import com.jetbrains.edu.jvm.environment.JdkLanguageEnvironment
import com.jetbrains.edu.jvm.environment.JdkLanguageEnvironmentNoOp
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase

abstract class JvmCourseGenerationTestBase : CourseGenerationTestBase<JdkLanguageEnvironment>() {
  override val defaultSettings: JdkLanguageEnvironment get() = JdkLanguageEnvironmentNoOp

  override fun tearDown() {
    try {
      JavaAwareProjectJdkTableImpl.removeInternalJdkInTests()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }
}
