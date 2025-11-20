package com.jetbrains.edu.socialMedia.marketplace

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.socialMedia.SocialMediaUtils
import com.jetbrains.edu.socialMedia.linkedIn.LinkedInPluginConfigurator
import com.jetbrains.edu.socialMedia.messages.EduSocialMediaBundle
import java.nio.file.Path
import kotlin.random.Random

class MarketplaceLinkedInConfigurator : LinkedInPluginConfigurator {

  override fun askToPost(project: Project, solvedTask: Task, statusBeforeCheck: CheckStatus): Boolean {
    return askToPost(solvedTask)
  }

  override fun getMessage(solvedTask: Task): String {
    return EduSocialMediaBundle.message("marketplace.linkedin.message", solvedTask.course.presentableName)
  }

  override fun getIndexWithImagePath(solvedTask: Task, imageIndex: Int?): Pair<Int, Path?> {
    val finalImageIndex = imageIndex ?: Random.nextInt(NUMBER_OF_GIFS)
    // TODO: move gifs out of `hyperskill` dir
    val imagePath = SocialMediaUtils.pluginRelativePath("socialMedia/linkedIn/hyperskill/achievement$finalImageIndex.gif")
    return finalImageIndex to imagePath
  }
}
