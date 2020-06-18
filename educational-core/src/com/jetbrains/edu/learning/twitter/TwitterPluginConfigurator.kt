package com.jetbrains.edu.learning.twitter

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.twitter.TwitterUtils.TwitterDialogPanel

/**
 * Provides twitting for courses
 *
 * @see TwitterAction
 */
interface TwitterPluginConfigurator {
  /**
   * The implementation should define policy when user will be asked to tweet.
   */
  fun askToTweet(project: Project, solvedTask: Task, statusBeforeCheck: CheckStatus): Boolean

  /**
   * @return panel that will be shown to user in ask to tweet dialog.
   */
  fun getTweetDialogPanel(solvedTask: Task): TwitterDialogPanel?

  companion object {
    @JvmField
    val EP_NAME = ExtensionPointName.create<TwitterPluginConfigurator>("Educational.twitterPluginConfigurator")
  }
}
