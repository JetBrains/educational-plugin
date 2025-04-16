package com.jetbrains.edu.learning.socialMedia.linkedIn

import com.intellij.openapi.project.Project
import java.nio.file.Path

object LinkedInUtils {

  fun doPost(project: Project, message: String, imagePath: Path) {
    if (LinkedInConnector.getInstance().account == null) {
      LinkedInConnector.getInstance().doAuthorize(
        {
          LinkedInConnector.getInstance().createPostWithMedia(project, message, imagePath)
        }
      )
    }
    else {
      LinkedInConnector.getInstance().createPostWithMedia(project, message, imagePath)
    }
  }
}