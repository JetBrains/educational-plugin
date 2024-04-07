package com.jetbrains.edu.jvm

import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl
import com.jetbrains.edu.learning.FindTaskFileTestBase

abstract class JvmFindTaskFileTestBase : FindTaskFileTestBase<JdkProjectSettings>() {

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