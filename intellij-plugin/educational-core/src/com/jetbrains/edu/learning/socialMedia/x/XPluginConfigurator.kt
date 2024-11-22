package com.jetbrains.edu.learning.socialMedia.x

import com.intellij.openapi.extensions.ExtensionPointName
import com.jetbrains.edu.learning.socialMedia.SocialMediaPluginConfigurator

interface XPluginConfigurator : SocialMediaPluginConfigurator {

  companion object {
    val EP_NAME = ExtensionPointName.create<XPluginConfigurator>("Educational.xPluginConfigurator")
  }
}
