package com.jetbrains.edu.learning.stepik.hyperskill.socialmedia.linkedIn

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.socialmedia.SocialMediaSettings
import com.jetbrains.edu.learning.socialmedia.SocialmediaUtils
import com.jetbrains.edu.learning.socialmedia.linkedIn.LinkedInPluginConfigurator
import com.jetbrains.edu.learning.socialmedia.linkedIn.LinkedInSettings
import com.jetbrains.edu.learning.socialmedia.linkedIn.LinkedInUtils
import com.jetbrains.edu.learning.stepik.hyperskill.socialmedia.HyperskillSocialmediaUtils
import com.jetbrains.edu.learning.stepik.hyperskill.socialmedia.HyperskillSocialmediaUtils.NUMBER_OF_GIFS
import java.nio.file.Path
import kotlin.random.Random

class HyperskillLinkedInConfigurator : LinkedInPluginConfigurator {

  override val settings: SocialMediaSettings<SocialMediaSettings.SocialMediaSettingsState> = LinkedInSettings.getInstance()

  override fun askToPost(project: Project, solvedTask: Task, statusBeforeCheck: CheckStatus): Boolean {
    return HyperskillSocialmediaUtils.askToPost(project, solvedTask, statusBeforeCheck)
  }

  override fun getMessage(solvedTask: Task): String {
    return HyperskillSocialmediaUtils.getMessage(solvedTask, "hyperskill.linkedin.message")
  }

  override fun getIndexWithImagePath(solvedTask: Task?, imageIndex: Int?): Pair<Int, Path?> {
    val gifIndex = imageIndex ?: Random.Default.nextInt(NUMBER_OF_GIFS)
    val imagePath = SocialmediaUtils.pluginRelativePath("socialmedia/linkedin/hyperskill/achievement$gifIndex.gif")
    return gifIndex to imagePath
  }

  override fun doPost(solvedTask: Task, imageIndex: Int?) {
    if (!settings.askToPost) return
    val (_, imagePath) = getIndexWithImagePath(null, imageIndex)
    imagePath ?: return
    LinkedInUtils.doPost(getMessage(solvedTask), imagePath)
  }
}
