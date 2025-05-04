package com.jetbrains.edu.socialMedia.linkedIn

import com.intellij.openapi.extensions.ExtensionPointName
import com.jetbrains.edu.socialMedia.SocialMediaPluginConfigurator

interface LinkedInPluginConfigurator : SocialMediaPluginConfigurator {

  companion object {
    val EP_NAME = ExtensionPointName.create<LinkedInPluginConfigurator>("Educational.linkedInPluginConfigurator")
  }
}
