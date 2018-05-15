@file:JvmName("EduVersions")

package com.jetbrains.edu.learning

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId

// If you change version of any format, add point about it in `docs/Versions.md`
const val XML_FORMAT_VERSION: Int = 9
const val JSON_FORMAT_VERSION: Int = 5

fun pluginVersion(pluginId: String): String? = PluginManager.getPlugin(PluginId.getId(pluginId))?.version
