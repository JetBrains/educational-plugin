package com.jetbrains.edu.learning.marketplace.changeHost

import com.jetbrains.edu.learning.isUnitTestMode
import org.jetbrains.annotations.TestOnly

private var MOCK: SubmissionsServiceChangeHostUI? = null

interface SubmissionsServiceChangeHostUI {
  fun getResultUrl(): String?
}

@TestOnly
fun withMockSubmissionsServiceChangeHostUI(mockUi: SubmissionsServiceChangeHostUI, action: () -> Unit) {
  try {
    MOCK = mockUi
    action()
  }
  finally {
    MOCK = null
  }
}

fun showDialogAndGetSubmissionsServiceHost(): String? {
  val ui = if (isUnitTestMode) {
    MOCK ?: error("Mock UI should be set via `withMockSubmissionsServiceChangeHostUI`")
  }
  else {
    SubmissionsServiceChangeHostDialog()
  }
  return ui.getResultUrl()
}
