package com.jetbrains.edu.learning.plugins

import com.intellij.ide.plugins.DynamicPluginListener.Companion.TOPIC
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.util.messages.MessageBusConnection

// BACKCOMPAT: 2019.2. Inline it
fun MessageBusConnection.subscribeOnDynamicPluginTopic(listener: DynamicPluginListener) {
  subscribe(TOPIC, object : com.intellij.ide.plugins.DynamicPluginListener {
    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
      listener.onPluginLoaded(pluginDescriptor)
    }
  })
}
