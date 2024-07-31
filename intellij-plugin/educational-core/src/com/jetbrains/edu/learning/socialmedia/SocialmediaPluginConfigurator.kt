package com.jetbrains.edu.learning.socialmedia

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.socialmedia.suggestToPostDialog.DefaultSuggestToPostDialogPanel
import com.jetbrains.edu.learning.socialmedia.suggestToPostDialog.SuggestToPostDialogPanel
import java.nio.file.Path

interface SocialmediaPluginConfigurator {
  /**
   * The implementation should define policy when user will be asked to tweet.
   */
  fun askToPost(project: Project, solvedTask: Task, statusBeforeCheck: CheckStatus): Boolean

  /**
   * @return panel that will be shown to user in ask to tweet dialog.
   */
  fun getPostDialogPanel(solvedTask: Task, imagePath: Path?, disposable: Disposable): SuggestToPostDialogPanel {
    return DefaultSuggestToPostDialogPanel(this, solvedTask, imagePath, disposable)
  }

  fun getDefaultMessage(solvedTask: Task): String
  fun getImagePath(solvedTask: Task): Path?

}
