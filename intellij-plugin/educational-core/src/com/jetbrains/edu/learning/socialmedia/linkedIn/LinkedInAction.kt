package com.jetbrains.edu.learning.socialmedia.linkedIn

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.socialmedia.SocialMediaSettings
import com.jetbrains.edu.learning.socialmedia.SocialmediaPostAction
import com.jetbrains.edu.learning.socialmedia.linkedIn.LinkedInUtils.createLinkedInDialogAndShow
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector

class LinkedInAction : SocialmediaPostAction<LinkedInPluginConfigurator>() {

  override val settings: SocialMediaSettings<SocialMediaSettings.SocialMediaSettingsState> = LinkedInSettings.getInstance()

  override fun createDialogAndShow(project: Project, configurator: LinkedInPluginConfigurator, task: Task) {
    createLinkedInDialogAndShow(project, configurator, task)
  }

  override fun sendStatistics(course: Course) {
    EduCounterUsageCollector.linkedInDialogShown(course)
  }

  override fun extensionPointName(): ExtensionPointName<LinkedInPluginConfigurator> {
    return LinkedInPluginConfigurator.EP_NAME
  }
}
