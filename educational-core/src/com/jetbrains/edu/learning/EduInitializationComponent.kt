package com.jetbrains.edu.learning

import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import java.util.*


class EduInitializationComponent : ApplicationComponent {
    override fun disposeComponent() {}

    override fun getComponentName() = "Educational.InitializationComponent"

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
        const val CONFLICTING_PLUGINS_DISABLED = "Educational.conflictingPluginsDisabled"
    }

    override fun initComponent() {
        if (PropertiesComponent.getInstance().isValueSet(CONFLICTING_PLUGINS_DISABLED) || ApplicationManager.getApplication().isUnitTestMode) {
            return
        }
        var disabledPlugins = disablePlugins()
        if (disabledPlugins.isNotEmpty()) {
            disabledPlugins = disabledPlugins.map { name -> "'$name'" }
            val ideName = ApplicationNamesInfo.getInstance().fullProductName
            val multiplePluginsDisabled = disabledPlugins.size != 1

            val names = if (multiplePluginsDisabled) StringUtil.join(disabledPlugins, ", ") else disabledPlugins[0]
            val ending = if (multiplePluginsDisabled) "s" else ""
            val verb = if (multiplePluginsDisabled) "were" else "was"
            val restartInfo = if (ApplicationManager.getApplication().isRestartCapable) "$ideName will be restarted" else "Restart $ideName"

            val message = "Conflicting plugin$ending $names $verb disabled. $restartInfo in order to apply changes"

            Messages.showInfoMessage(message, restartInfo)
            ApplicationManager.getApplication().restart()
        } else {
            PropertiesComponent.getInstance().setValue(CONFLICTING_PLUGINS_DISABLED, "true")
        }

    }


    private fun disablePlugins(): List<String> {
        val disabledPlugins = ArrayList<String>()
        for (id in IDS) {
            val plugin = PluginManager.getPlugin(PluginId.getId(id))
            plugin ?: continue
            if (plugin.isEnabled) {
                disabledPlugins.add(plugin.name)
                PluginManagerCore.disablePlugin(id)
            }
        }
        return disabledPlugins
    }
}