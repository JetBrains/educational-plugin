package com.jetbrains.edu.learning.socialmedia.linkedIn

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.runInBackground
import com.jetbrains.edu.learning.socialmedia.linkedIn.dialog.LinkedInDialog

object LinkedInUtils {

  fun createLinkedInDialogAndShow(project: Project, configurator: LinkedInPluginConfigurator, task: Task) {
    project.invokeLater {
      val imagePath = configurator.getImagePath(task) ?: return@invokeLater
      val dialog = LinkedInDialog(project) { configurator.getPostDialogPanel(task, imagePath, it) }
      if (dialog.showAndGet()) {
        runInBackground(project, EduCoreBundle.message("linkedin.loading.posting")) {
          if (LinkedInConnector.getInstance().account == null) {
            LinkedInConnector.getInstance().doAuthorize(
              { LinkedInConnector.getInstance().createPostWithMedia(dialog.message, imagePath) }
            )
          }
          else {
            LinkedInConnector.getInstance().createPostWithMedia(dialog.message, imagePath)
          }
        }
      }
    }
  }
}