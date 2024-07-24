package com.jetbrains.edu.learning.socialmedia.twitter.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.isUnitTestMode
import org.jetbrains.annotations.TestOnly

interface TwitterDialogUI {
  val message: String
  fun showAndGet(): Boolean
}

fun createTwitterDialogUI(project: Project, dialogPanelCreator: (Disposable) -> TwitterDialogPanel): TwitterDialogUI {
  return if (isUnitTestMode) {
    MOCK ?: error("You should set mock UI via `withMockTwitterDialogUI`")
  }
  else {
    TwitterDialog(project, dialogPanelCreator)
  }
}

private var MOCK: TwitterDialogUI? = null

@TestOnly
fun withMockTwitterDialogUI(mockUI: TwitterDialogUI, action: () -> Unit) {
  try {
    MOCK = mockUI
    action()
  }
  finally {
    MOCK = null
  }
}
