package com.jetbrains.edu.learning

import org.jetbrains.annotations.VisibleForTesting
import java.util.concurrent.atomic.AtomicBoolean

@VisibleForTesting
object EduProjectServiceForTests {
  private val reopenEditorHelpExecuted = AtomicBoolean(false)

  fun isReopenEditorHelpExecuted(): Boolean = reopenEditorHelpExecuted.get()

  fun setReopenEditorHelpExecuted(value: Boolean) {
    reopenEditorHelpExecuted.set(value)
  }

  fun reset() {
    reopenEditorHelpExecuted.set(false)
  }
}
