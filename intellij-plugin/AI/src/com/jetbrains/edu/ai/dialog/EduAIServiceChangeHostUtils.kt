package com.jetbrains.edu.ai.dialog

import com.jetbrains.edu.learning.isUnitTestMode
import org.jetbrains.annotations.TestOnly

private var MOCK: EduAIServiceChangeHostUI? = null

interface EduAIServiceChangeHostUI {
  fun getResultUrl(): String?
}

@TestOnly
fun withMockEduAIServiceChangeHostUI(mockUi: EduAIServiceChangeHostUI, action: () -> Unit) {
  try {
    MOCK = mockUi
    action()
  }
  finally {
    MOCK = null
  }
}

fun showDialogAndGetAIServiceHost(): String? {
  val ui = if (isUnitTestMode) {
    MOCK ?: error("Mock UI should be set via `withMockEduAIServiceChangeHostUI`")
  }
  else {
    EduAIServiceChangeHostDialog()
  }
  return ui.getResultUrl()
}