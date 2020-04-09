package com.jetbrains.edu.learning.plugins

import com.intellij.ide.plugins.IdeaPluginDescriptor

// BACKCOMPAT: 2019.2. Use com.intellij.ide.plugins.DynamicPluginListener instead
interface DynamicPluginListener {
  fun onPluginLoaded(pluginDescriptor: IdeaPluginDescriptor)
}
