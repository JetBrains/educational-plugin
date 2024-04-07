package com.jetbrains.edu.jvm.courseGeneration

import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase

abstract class JvmCourseGenerationTestBase : CourseGenerationTestBase<JdkProjectSettings>() {
  override val defaultSettings: JdkProjectSettings get() = JdkProjectSettings.emptySettings()

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
