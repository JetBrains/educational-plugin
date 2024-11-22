package com.jetbrains.edu.learning.stepik.hyperskill.socialMedia.linkedIn

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.socialMedia.SocialMediaSettings
import com.jetbrains.edu.learning.socialMedia.SocialMediaUtils
import com.jetbrains.edu.learning.socialMedia.linkedIn.LinkedInPluginConfigurator
import com.jetbrains.edu.learning.socialMedia.linkedIn.LinkedInSettings
import com.jetbrains.edu.learning.socialMedia.linkedIn.LinkedInUtils
import com.jetbrains.edu.learning.stepik.hyperskill.socialMedia.HyperskillSocialMediaUtils
import com.jetbrains.edu.learning.stepik.hyperskill.socialMedia.HyperskillSocialMediaUtils.NUMBER_OF_GIFS
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
    val imagePath = SocialMediaUtils.pluginRelativePath("socialMedia/linkedin/hyperskill/achievement$finalImageIndex.gif")
    return finalImageIndex to imagePath
  }

  override fun doPost(solvedTask: Task, imageIndex: Int?) {
    if (!settings.askToPost) return
    val (_, imagePath) = getIndexWithImagePath(null, imageIndex)
    imagePath ?: return
    LinkedInUtils.doPost(getMessage(solvedTask), imagePath)
  }
}
