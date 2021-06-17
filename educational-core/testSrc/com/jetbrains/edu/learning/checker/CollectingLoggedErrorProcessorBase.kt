package com.jetbrains.edu.learning.checker

import com.intellij.testFramework.LoggedErrorProcessor
import java.util.*

abstract class CollectingLoggedErrorProcessorBase : LoggedErrorProcessor() {

  protected val _exceptions: MutableList<AssertionError> = Collections.synchronizedList(mutableListOf<AssertionError>())

  val exceptions: List<AssertionError> get() = _exceptions
}
