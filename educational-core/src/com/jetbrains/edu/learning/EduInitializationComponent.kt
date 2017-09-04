package com.jetbrains.edu.learning

import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.ui.Messages


class EduInitializationComponent : ApplicationComponent {
    companion object {
        @JvmField
        val IDS = arrayOf(
                "com.jetbrains.edu.intellij",
                "com.jetbrains.edu.interactivelearning",
                "com.jetbrains.python.edu.interactivelearning.python",
                "com.jetbrains.edu.coursecreator",
                "com.jetbrains.edu.coursecreator.python",
                "com.jetbrains.edu.kotlin",
                "com.jetbrains.edu.coursecreator.intellij",
                "com.jetbrains.edu.java",
                "com.jetbrains.python.edu.core",
                "com.jetbrains.edu.core"
        )
    }

    override fun initComponent() {
        if (disablePlugins()) {
            Messages.showInfoMessage("IDE will be restarted", "Restart")
            ApplicationManager.getApplication().restart()
        }
    }


    private fun disablePlugins(): Boolean {
        var restartNeeded = false
        for (id in IDS) {
            val plugin = PluginManager.getPlugin(PluginId.getId(id))
            plugin ?: continue
            if (plugin.isEnabled) {
                PluginManagerCore.disablePlugin(id)
                restartNeeded = true
            }
        }
        return restartNeeded
    }
}