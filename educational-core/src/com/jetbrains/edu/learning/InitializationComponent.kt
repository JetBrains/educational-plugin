package com.jetbrains.edu.learning

import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.editor.EduEditorFactoryListener
import com.jetbrains.edu.learning.stepik.update.NewCoursesNotifier
import java.util.*

class InitializationComponent : ApplicationComponent {

    private val newCoursesNotifier = NewCoursesNotifier(ApplicationManager.getApplication())

    override fun initComponent() {
        //Register placeholder size listener
        EditorFactory.getInstance().addEditorFactoryListener(EduEditorFactoryListener(), ApplicationManager.getApplication())

        if (isUnitTestMode) return
        if (PropertiesComponent.getInstance().isValueSet(CONFLICTING_PLUGINS_DISABLED)) {
            newCoursesNotifier.scheduleNotification()
            return
        }

        // Remove conflicting plugins
        var disabledPlugins = disablePlugins()
        if (disabledPlugins.isNotEmpty()) {
            disabledPlugins = disabledPlugins.map { name -> "'$name'" }
            val multiplePluginsDisabled = disabledPlugins.size != 1

            val names = if (multiplePluginsDisabled) StringUtil.join(disabledPlugins, ", ") else disabledPlugins[0]
            val ending = if (multiplePluginsDisabled) "s" else ""
            val verb = if (multiplePluginsDisabled) "were" else "was"

            restartIDE("Conflicting plugin$ending $names $verb disabled")
        } else {
            PropertiesComponent.getInstance().setValue(CONFLICTING_PLUGINS_DISABLED, "true")
            newCoursesNotifier.scheduleNotification()
        }
    }

    private fun disablePlugins(): List<String> {
        val disabledPlugins = ArrayList<String>()
        for (id in IDS) {
            val plugin = PluginManager.getPlugin(PluginId.getId(id)) ?: continue
            if (plugin.isEnabled) {
                disabledPlugins.add(plugin.name)
                PluginManagerCore.disablePlugin(id)
            }
        }
        return disabledPlugins
    }

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
}
