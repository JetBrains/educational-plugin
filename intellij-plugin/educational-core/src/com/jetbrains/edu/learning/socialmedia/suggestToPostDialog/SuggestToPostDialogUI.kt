package com.jetbrains.edu.learning.socialmedia.suggestToPostDialog

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.socialmedia.SocialmediaPluginConfigurator
import org.jetbrains.annotations.TestOnly
import java.nio.file.Path

interface SuggestToPostDialogUI {
  fun showAndGet(): Boolean
}

fun createSuggestToPostDialogUI(
  project: Project,
  configurators: List<SocialmediaPluginConfigurator>,
  message: String,
  imagePath: Path?
): SuggestToPostDialogUI {
  return if (isUnitTestMode) {
    MOCK ?: error("You should set mock UI via `withMockSuggestToDialogUI`")
  }
  else {
    SuggestToPostDialog(project, configurators, message, imagePath)
  }
}

private var MOCK: SuggestToPostDialogUI? = null

@TestOnly
fun withMockSuggestToPostDialogUI(mockUI: SuggestToPostDialogUI, action: () -> Unit) {
  try {
    MOCK = mockUI
    action()
  }
  finally {
    MOCK = null
  }
}
