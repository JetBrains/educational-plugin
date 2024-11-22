package com.jetbrains.edu.learning.socialMedia.suggestToPostDialog

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.socialMedia.SocialMediaPluginConfigurator
import org.jetbrains.annotations.TestOnly
import org.jetbrains.annotations.VisibleForTesting
import java.nio.file.Path

interface SuggestToPostDialogUI {
  fun showAndGet(): Boolean

  @VisibleForTesting
  fun setConfigurators(configurators: List<SocialMediaPluginConfigurator>) {
  }
}

fun createSuggestToPostDialogUI(
  project: Project,
  configurators: List<SocialMediaPluginConfigurator>,
  message: String,
  imagePath: Path?
): SuggestToPostDialogUI {
  return if (isUnitTestMode) {
    val mockUI = MOCK
    if (mockUI == null) error("You should set mock UI via `withMockSuggestToDialogUI`")
    mockUI.setConfigurators(configurators)
    mockUI
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
