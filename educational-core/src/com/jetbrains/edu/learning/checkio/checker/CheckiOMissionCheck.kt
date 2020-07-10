package com.jetbrains.edu.learning.checkio.checker

import com.jetbrains.edu.learning.checker.CheckResult
import java.util.concurrent.Callable
import javax.swing.JComponent

abstract class CheckiOMissionCheck : Callable<CheckResult> {
  abstract override fun call(): CheckResult

  abstract fun getPanel(): JComponent
}
