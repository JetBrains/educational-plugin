package com.jetbrains.edu.java

import com.jetbrains.edu.jvm.gradle.checker.GradleTaskCheckerProvider
import com.jetbrains.edu.learning.checker.CodeExecutor

class JTaskCheckerProvider : GradleTaskCheckerProvider() {
  override fun getCodeExecutor(): CodeExecutor = JCodeExecutor()
}
