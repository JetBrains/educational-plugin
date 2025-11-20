package com.jetbrains.edu.socialMedia.x

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.socialMedia.SocialMediaPluginConfigurator
import com.jetbrains.edu.socialMedia.SocialMediaSettings

interface XPluginConfigurator : SocialMediaPluginConfigurator {

  override val settings: SocialMediaSettings<SocialMediaSettings.SocialMediaSettingsState>
    get() = XSettings.getInstance()

  override fun doPost(project: Project, solvedTask: Task, imageIndex: Int?) {
    if (!settings.askToPost) return
    val (_, imagePath) = getIndexWithImagePath(solvedTask, imageIndex)
    XUtils.doPost(project, getMessage(solvedTask), imagePath)
  }

  companion object {
    val EP_NAME = ExtensionPointName.create<XPluginConfigurator>("Educational.xPluginConfigurator")
  }
}
