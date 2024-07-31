package com.jetbrains.edu.learning.socialmedia.twitter.dialog

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.socialmedia.suggestToPostDialog.SuggestToPostDialogPanel
import com.jetbrains.edu.learning.socialmedia.suggestToPostDialog.SuggestToPostDialogUI
import org.jetbrains.annotations.TestOnly

fun createTwitterDialogUI(project: Project, dialogPanelCreator: (Disposable) -> SuggestToPostDialogPanel): SuggestToPostDialogUI {
  return if (isUnitTestMode) {
    MOCK ?: error("You should set mock UI via `withMockTwitterDialogUI`")
  }
  else {
    TwitterDialog(project, dialogPanelCreator)
  }
}

private var MOCK: SuggestToPostDialogUI? = null

@TestOnly
fun withMockTwitterDialogUI(mockUI: SuggestToPostDialogUI, action: () -> Unit) {
  try {
    MOCK = mockUI
    action()
  }
  finally {
    MOCK = null
  }
}
