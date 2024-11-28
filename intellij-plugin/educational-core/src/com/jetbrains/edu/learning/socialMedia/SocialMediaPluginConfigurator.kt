package com.jetbrains.edu.learning.socialMedia

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
interface SocialMediaPluginConfigurator {

  val settings: SocialMediaSettings<SocialMediaSettings.SocialMediaSettingsState>

  /**
   * The implementation should define policy when user will be asked to post.
   *
   * Make sure that if you return 'true' for some task, then all other existing
   * implementations of SocialMediaPluginConfigurator that also return true for
   * that task, have the same number of images, and those images correspond to
   * each other.
   *
   * For example, if you implement a Configurator for hyperskill courses, it
   * should have the same images as other hyperskill configurators.
   * If you implement a configurator for the "Kotlin Koans" course, it should
   * have the same images as existing "Kotlin Koans" configurators.
   */
  fun askToPost(project: Project, solvedTask: Task, statusBeforeCheck: CheckStatus): Boolean

  fun getMessage(solvedTask: Task): String
  fun getIndexWithImagePath(solvedTask: Task?, imageIndex: Int? = null): Pair<Int?, Path?>
  fun doPost(solvedTask: Task, imageIndex: Int?)

}
