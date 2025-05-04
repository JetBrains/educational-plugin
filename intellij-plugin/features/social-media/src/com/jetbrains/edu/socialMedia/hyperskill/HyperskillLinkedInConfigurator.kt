package com.jetbrains.edu.socialMedia.hyperskill

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.socialMedia.SocialMediaSettings
import com.jetbrains.edu.socialMedia.SocialMediaUtils
import com.jetbrains.edu.socialMedia.hyperskill.HyperskillSocialMediaUtils.NUMBER_OF_GIFS
import com.jetbrains.edu.socialMedia.linkedIn.LinkedInPluginConfigurator
import com.jetbrains.edu.socialMedia.linkedIn.LinkedInSettings
import com.jetbrains.edu.socialMedia.linkedIn.LinkedInUtils
import java.nio.file.Path
import kotlin.random.Random

class HyperskillLinkedInConfigurator : LinkedInPluginConfigurator {

  override val settings: SocialMediaSettings<SocialMediaSettings.SocialMediaSettingsState> = LinkedInSettings.getInstance()

  override fun askToPost(project: Project, solvedTask: Task, statusBeforeCheck: CheckStatus): Boolean {
    return HyperskillSocialMediaUtils.askToPost(project, solvedTask, statusBeforeCheck)
  }

  override fun getMessage(solvedTask: Task): String {
    return HyperskillSocialMediaUtils.getMessage(solvedTask, "hyperskill.linkedin.message")
  }

  override fun getIndexWithImagePath(solvedTask: Task?, imageIndex: Int?): Pair<Int, Path?> {
    val finalImageIndex = imageIndex ?: Random.Default.nextInt(NUMBER_OF_GIFS)
    val imagePath = SocialMediaUtils.pluginRelativePath("socialMedia/linkedIn/hyperskill/achievement$finalImageIndex.gif")
    return finalImageIndex to imagePath
  }

  override fun doPost(project: Project, solvedTask: Task, imageIndex: Int?) {
    if (!settings.askToPost) return
    val (_, imagePath) = getIndexWithImagePath(null, imageIndex)
    imagePath ?: return
    LinkedInUtils.doPost(project, getMessage(solvedTask), imagePath)
  }
}
