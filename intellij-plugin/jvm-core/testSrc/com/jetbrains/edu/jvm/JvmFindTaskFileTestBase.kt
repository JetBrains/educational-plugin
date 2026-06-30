package com.jetbrains.edu.jvm

import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl
import com.jetbrains.edu.jvm.environment.JdkLanguageEnvironment
import com.jetbrains.edu.jvm.environment.JdkLanguageEnvironmentNoOp
import com.jetbrains.edu.learning.FindTaskFileTestBase

abstract class JvmFindTaskFileTestBase : FindTaskFileTestBase<JdkLanguageEnvironment>() {

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