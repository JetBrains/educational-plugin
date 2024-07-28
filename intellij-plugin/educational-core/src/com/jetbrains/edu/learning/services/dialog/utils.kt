package com.jetbrains.edu.learning.services.dialog

import com.jetbrains.edu.learning.isUnitTestMode
import org.jetbrains.annotations.TestOnly

private var MOCK: ServiceHostChanger? = null

@TestOnly
fun withMockServiceHostChanger(mockUi: ServiceHostChanger, action: () -> Unit) {
  try {
    MOCK = mockUi
    action()
  }
  finally {
    MOCK = null
  }
}

fun <T : ServiceHostChanger> T.showDialogAndGetHost(): String? {
  val ui = if (isUnitTestMode) {
    MOCK ?: error("Mock UI should be set via `withMockServiceHostChanger`")
  }
  else {
    this
  }
  return ui.getResultUrl()
}