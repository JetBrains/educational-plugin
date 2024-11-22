package com.jetbrains.edu.learning.socialMedia.linkedIn

import java.nio.file.Path

object LinkedInUtils {

  fun doPost(message: String, imagePath: Path) {
    if (LinkedInConnector.getInstance().account == null) {
      LinkedInConnector.getInstance().doAuthorize(
        {
          LinkedInConnector.getInstance().createPostWithMedia(message, imagePath)
        }
      )
    }
    else {
      LinkedInConnector.getInstance().createPostWithMedia(message, imagePath)
    }
  }
}