package com.jetbrains.edu.learning.socialmedia

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.nio.file.Path


/**
 * To support new social network:
 *
 * 1. Extend this interface and add EP_NAME.
 * 2. Implement interface from Step 1 for the corresponding case (for example, Hyperskill or Kotlin Koans course).
 */
interface SocialmediaPluginConfigurator {

  val settings: SocialMediaSettings<SocialMediaSettings.SocialMediaSettingsState>

  /**
   * The implementation should define policy when user will be asked to post.
   */
  fun askToPost(project: Project, solvedTask: Task, statusBeforeCheck: CheckStatus): Boolean

  fun getMessage(solvedTask: Task): String
  fun getIndexWithImagePath(solvedTask: Task?, imageIndex: Int? = null): Pair<Int?, Path?>
  fun doPost(solvedTask: Task, imageIndex: Int?)

}
