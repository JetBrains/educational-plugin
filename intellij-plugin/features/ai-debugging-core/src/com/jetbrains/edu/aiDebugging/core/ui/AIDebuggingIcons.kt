package com.jetbrains.edu.aiDebugging.core.ui

import com.intellij.openapi.util.IconLoader.getIcon
import javax.swing.Icon

object AIDebuggingIcons {

  val AIHint: Icon = load("/icons/hint.svg")

  /**
   * @param path the path to the icon in the `resources` directory.
   * @return the loaded [Icon] object.
   */
  @Suppress("SameParameterValue")
  private fun load(path: String): Icon {
    return getIcon(path, AIDebuggingIcons::class.java)
  }
}