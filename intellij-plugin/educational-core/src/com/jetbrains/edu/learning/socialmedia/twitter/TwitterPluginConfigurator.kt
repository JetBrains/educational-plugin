package com.jetbrains.edu.learning.socialmedia.twitter

import com.intellij.openapi.extensions.ExtensionPointName
import com.jetbrains.edu.learning.socialmedia.SocialmediaPluginConfigurator

/**
 * Provides twitting for courses
 *
 * @see TwitterAction
 */
interface TwitterPluginConfigurator : SocialmediaPluginConfigurator {

  companion object {
    val EP_NAME = ExtensionPointName.create<TwitterPluginConfigurator>("Educational.twitterPluginConfigurator")
  }
}
