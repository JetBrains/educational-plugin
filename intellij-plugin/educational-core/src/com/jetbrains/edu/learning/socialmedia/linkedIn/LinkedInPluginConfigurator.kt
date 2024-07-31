package com.jetbrains.edu.learning.socialmedia.linkedIn

import com.intellij.openapi.extensions.ExtensionPointName
import com.jetbrains.edu.learning.socialmedia.SocialmediaPluginConfigurator

interface LinkedInPluginConfigurator : SocialmediaPluginConfigurator {

  companion object {
    val EP_NAME = ExtensionPointName.create<LinkedInPluginConfigurator>("Educational.linkedInPluginConfigurator")
  }
}
