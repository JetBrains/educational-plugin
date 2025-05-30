package com.jetbrains.edu.learning

import com.intellij.ide.plugins.IdeaPluginDescriptorImpl

internal fun collectFromModules(
  pluginDescriptor: IdeaPluginDescriptorImpl,
  collect: (moduleDescriptor: IdeaPluginDescriptorImpl) -> Unit
) {
  for (module in pluginDescriptor.content.modules) {
    collect(module.requireDescriptor())
  }
}