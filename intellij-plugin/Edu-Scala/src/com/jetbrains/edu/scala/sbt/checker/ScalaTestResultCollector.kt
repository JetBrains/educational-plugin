package com.jetbrains.edu.scala.sbt.checker

import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.jetbrains.edu.learning.checker.tests.SMTestResultCollector
import com.jetbrains.edu.learning.xmlEscaped

class ScalaTestResultCollector : SMTestResultCollector() {
  override fun getErrorMessage(node: SMTestProxy): String = super.getErrorMessage(node).xmlEscaped
}
