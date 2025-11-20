package com.jetbrains.edu.socialMedia.hyperskill

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.socialMedia.SocialMediaUtils
import com.jetbrains.edu.socialMedia.hyperskill.HyperskillSocialMediaUtils.NUMBER_OF_GIFS
import com.jetbrains.edu.socialMedia.linkedIn.LinkedInPluginConfigurator
import java.nio.file.Path
import kotlin.random.Random

class HyperskillLinkedInConfigurator : LinkedInPluginConfigurator {

  override fun askToPost(project: Project, solvedTask: Task, statusBeforeCheck: CheckStatus): Boolean {
    return HyperskillSocialMediaUtils.askToPost(project, solvedTask, statusBeforeCheck)
  }

  override fun getMessage(solvedTask: Task): String {
    return HyperskillSocialMediaUtils.getMessage(solvedTask, "hyperskill.linkedin.message")
  }

  override fun getIndexWithImagePath(solvedTask: Task, imageIndex: Int?): Pair<Int, Path?> {
    val finalImageIndex = imageIndex ?: Random.Default.nextInt(NUMBER_OF_GIFS)
    val imagePath = SocialMediaUtils.pluginRelativePath("socialMedia/linkedIn/hyperskill/achievement$finalImageIndex.gif")
    return finalImageIndex to imagePath
  }
}
