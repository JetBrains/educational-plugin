package com.jetbrains.edu.socialMedia.linkedIn

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.socialMedia.SocialMediaPluginConfigurator
import com.jetbrains.edu.socialMedia.SocialMediaSettings

interface LinkedInPluginConfigurator : SocialMediaPluginConfigurator {

  override val settings: SocialMediaSettings<SocialMediaSettings.SocialMediaSettingsState>
    get() = LinkedInSettings.getInstance()

  override fun doPost(project: Project, solvedTask: Task, imageIndex: Int?) {
    if (!settings.askToPost) return
    val imagePath = getIndexWithImagePath(solvedTask, imageIndex).second ?: return
    LinkedInUtils.doPost(project, getMessage(solvedTask), imagePath)
  }

  companion object {
    val EP_NAME = ExtensionPointName.create<LinkedInPluginConfigurator>("Educational.linkedInPluginConfigurator")
  }
}
