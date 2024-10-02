package com.jetbrains.edu.learning.socialmedia.suggestToPostDialog

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.socialmedia.SocialmediaPluginConfigurator
import org.jetbrains.annotations.TestOnly
import java.nio.file.Path

interface SuggestToPostDialogUI {
  fun showAndGet(): Boolean
}

fun createTwitterDialogUI(
  project: Project,
  configurators: List<SocialmediaPluginConfigurator>,
  configurator: SocialmediaPluginConfigurator,
  task: Task,
  imagePath: Path?
): SuggestToPostDialogUI {
  return if (isUnitTestMode) {
    MOCK ?: error("You should set mock UI via `withMockTwitterDialogUI`")
  }
  else {
    SuggestToPostDialog(project, configurators) { configurator.getPostDialogPanel(task, imagePath, it) }
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
