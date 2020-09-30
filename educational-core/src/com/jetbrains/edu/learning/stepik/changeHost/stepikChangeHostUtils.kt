package com.jetbrains.edu.learning.stepik.changeHost

import com.jetbrains.edu.learning.isUnitTestMode
import org.jetbrains.annotations.TestOnly

private var MOCK: StepikChangeHostUI? = null

fun showStepikChangeHostDialog(): StepikHost? {
  val ui = if (isUnitTestMode) {
    MOCK ?: error("Mock UI should be set via `withMockStepikChangeHostUI`")
  }
  else {
    DialogStepikChangeHostUI()
  }
  return ui.showDialog()
}

@TestOnly
fun withMockStepikChangeHostUI(mockUi: StepikChangeHostUI, action: () -> Unit) {
  try {
    MOCK = mockUi
    action()
  }
  finally {
    MOCK = null
  }
}

interface StepikChangeHostUI {
  fun showDialog(): StepikHost?
}

class DialogStepikChangeHostUI : StepikChangeHostUI {
  override fun showDialog(): StepikHost? {
    val dialog = StepikChangeHostDialog()
    return if (dialog.showAndGet()) dialog.getSelectedItem() else null
  }
}