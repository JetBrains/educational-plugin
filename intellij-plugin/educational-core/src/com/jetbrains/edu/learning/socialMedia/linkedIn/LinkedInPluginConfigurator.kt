package com.jetbrains.edu.learning.socialMedia.linkedIn

import com.intellij.openapi.extensions.ExtensionPointName
import com.jetbrains.edu.learning.socialMedia.SocialMediaPluginConfigurator

interface LinkedInPluginConfigurator : SocialMediaPluginConfigurator {

  companion object {
    val EP_NAME = ExtensionPointName.create<LinkedInPluginConfigurator>("Educational.linkedInPluginConfigurator")
  }
}
