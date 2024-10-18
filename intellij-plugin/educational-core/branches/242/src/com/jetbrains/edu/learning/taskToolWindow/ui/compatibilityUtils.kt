package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.ui.jcef.JCEFHtmlPanel


// BACKCOMPAT: 2024.2 drop it
fun JCEFHtmlPanel.disableNavigation() =
  javaClass.kotlin.members.firstOrNull { it.name == "disableNavigation" && it.parameters.size == 1 }?.call(this)
