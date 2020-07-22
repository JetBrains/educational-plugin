package com.jetbrains.edu.learning

import com.intellij.ide.plugins.CannotUnloadPluginException
import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor

class EduDynamicPluginListener : DynamicPluginListener {

  override fun checkUnloadPlugin(pluginDescriptor: IdeaPluginDescriptor) {
    if (pluginDescriptor.pluginId.idString == EduNames.PLUGIN_ID) {
      throw CannotUnloadPluginException("EduTools plugin unloading is not supported yet")
    }
  }
}
