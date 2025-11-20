package com.jetbrains.edu.socialMedia.marketplace

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.socialMedia.SocialMediaUtils
import com.jetbrains.edu.socialMedia.messages.EduSocialMediaBundle
import com.jetbrains.edu.socialMedia.x.XPluginConfigurator
import java.nio.file.Path
import kotlin.random.Random

class MarketplaceXConfigurator : XPluginConfigurator {

  override fun askToPost(project: Project, solvedTask: Task, statusBeforeCheck: CheckStatus): Boolean {
    return askToPost(solvedTask)
  }

  override fun getMessage(solvedTask: Task): String {
    return EduSocialMediaBundle.message("marketplace.x.message", solvedTask.course.presentableName)
  }

  override fun getIndexWithImagePath(solvedTask: Task, imageIndex: Int?): Pair<Int, Path?> {
    val finalImageIndex = imageIndex ?: Random.nextInt(NUMBER_OF_GIFS)
    // TODO: move gifs out of `hyperskill` dir
    val imagePath = SocialMediaUtils.pluginRelativePath("socialMedia/x/hyperskill/achievement$finalImageIndex.gif")
    return finalImageIndex to imagePath
  }
}
