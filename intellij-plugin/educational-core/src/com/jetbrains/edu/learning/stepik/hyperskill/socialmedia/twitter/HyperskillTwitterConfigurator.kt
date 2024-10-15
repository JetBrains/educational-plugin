package com.jetbrains.edu.learning.stepik.hyperskill.socialmedia.twitter

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.socialmedia.SocialMediaSettings
import com.jetbrains.edu.learning.socialmedia.SocialmediaUtils
import com.jetbrains.edu.learning.socialmedia.twitter.TwitterPluginConfigurator
import com.jetbrains.edu.learning.socialmedia.twitter.TwitterSettings
import com.jetbrains.edu.learning.socialmedia.twitter.TwitterUtils
import com.jetbrains.edu.learning.stepik.hyperskill.socialmedia.HyperskillSocialmediaUtils
import com.jetbrains.edu.learning.stepik.hyperskill.socialmedia.HyperskillSocialmediaUtils.NUMBER_OF_GIFS
import java.nio.file.Path
import kotlin.random.Random

class HyperskillTwitterConfigurator : TwitterPluginConfigurator {

  override val settings: SocialMediaSettings<SocialMediaSettings.SocialMediaSettingsState> = TwitterSettings.getInstance()

  override fun askToPost(project: Project, solvedTask: Task, statusBeforeCheck: CheckStatus): Boolean {
    return HyperskillSocialmediaUtils.askToPost(project, solvedTask, statusBeforeCheck)
  }

  override fun getMessage(solvedTask: Task): String {
    return HyperskillSocialmediaUtils.getMessage(solvedTask, "hyperskill.twitter.message")
  }

  override fun getIndexWithImagePath(solvedTask: Task?, imageIndex: Int?): Pair<Int, Path?> {
    val finalImageIndex = imageIndex ?: Random.Default.nextInt(NUMBER_OF_GIFS)
    val imagePath = SocialmediaUtils.pluginRelativePath("socialmedia/twitter/hyperskill/achievement$finalImageIndex.gif")
    return finalImageIndex to imagePath
  }

  override fun doPost(solvedTask: Task, imageIndex: Int?) {
    if (!settings.askToPost) return
    val (_, imagePath) = getIndexWithImagePath(null, imageIndex)
    TwitterUtils.doPost(getMessage(solvedTask), imagePath)
  }
}
