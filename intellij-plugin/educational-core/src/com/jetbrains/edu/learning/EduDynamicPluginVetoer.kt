package com.jetbrains.edu.learning

import com.intellij.ide.plugins.DynamicPluginVetoer
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.Nls

class EduDynamicPluginVetoer : DynamicPluginVetoer {
  override fun vetoPluginUnload(pluginDescriptor: IdeaPluginDescriptor): @Nls String? {
    if (pluginDescriptor.pluginId.idString == EduNames.PLUGIN_ID) {
      return EduCoreBundle.message("plugin.cannot.be.dynamically.unloaded")
    }
    return null
  }
}
