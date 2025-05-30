package com.jetbrains.edu.learning

import com.intellij.ide.plugins.IdeaPluginDescriptorImpl
import com.intellij.ide.plugins.contentModules
import com.intellij.ide.plugins.getMainDescriptor

internal fun collectFromModules(
  pluginDescriptor: IdeaPluginDescriptorImpl,
  collect: (moduleDescriptor: IdeaPluginDescriptorImpl) -> Unit
) {
  for (module in pluginDescriptor.contentModules) {
    collect(module.getMainDescriptor())
  }
}