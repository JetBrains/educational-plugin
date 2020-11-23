package com.jetbrains.edu.scala.gradle

import com.jetbrains.edu.jvm.gradle.checker.GradleTaskCheckerProvider
import com.jetbrains.edu.learning.checker.CodeExecutor

class ScalaTaskCheckerProvider : GradleTaskCheckerProvider() {
  override fun getCodeExecutor(): CodeExecutor = ScalaCodeExecutor()
}
