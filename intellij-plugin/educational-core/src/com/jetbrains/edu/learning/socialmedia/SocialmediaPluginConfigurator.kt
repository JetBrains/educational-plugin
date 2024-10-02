package com.jetbrains.edu.learning.socialmedia

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.socialmedia.suggestToPostDialog.SuggestToPostDialogPanel
import java.nio.file.Path

interface SocialmediaPluginConfigurator {

  val settings: SocialMediaSettings<SocialMediaSettings.SocialMediaSettingsState>

  /**
   * The implementation should define policy when user will be asked to post.
   */
  fun askToPost(project: Project, solvedTask: Task, statusBeforeCheck: CheckStatus): Boolean

  /**
   * @return panel that will be shown to user in ask to post dialog.
   */
  fun getPostDialogPanel(solvedTask: Task, imagePath: Path?, disposable: Disposable): SuggestToPostDialogPanel {
    return SuggestToPostDialogPanel(this, solvedTask, imagePath, disposable)
  }

  fun getMessage(solvedTask: Task): String
  fun getIndexWithImagePath(solvedTask: Task?, imageIndex: Int? = null): Pair<Int?, Path?>
  fun doPost(solvedTask: Task, imageIndex: Int?)

}
