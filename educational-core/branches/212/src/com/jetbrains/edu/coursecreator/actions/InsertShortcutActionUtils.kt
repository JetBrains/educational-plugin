package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.ex.QuickListsManager

val quickListsManagerInstance: QuickListsManager
  get() = QuickListsManager.getInstance()