package com.jetbrains.edu.learning

import com.intellij.ide.plugins.CannotUnloadPluginException
import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.extensions.PluginId

class EduDynamicPluginListener : DynamicPluginListener {

  override fun checkUnloadPlugin(pluginDescriptor: IdeaPluginDescriptor) {
    if (pluginDescriptor.pluginId == PluginId.getId(EduNames.PLUGIN_ID)) {
      throw CannotUnloadPluginException("JetBrains Academy plugin unloading is not supported yet")
    }
  }
}
