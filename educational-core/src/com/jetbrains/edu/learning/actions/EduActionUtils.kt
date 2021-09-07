package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import org.jetbrains.annotations.NonNls

object EduActionUtils {

  fun getAction(@NonNls id: String): AnAction {
    return ActionManager.getInstance().getAction(id) ?: error("Can not find action by id $id")
  }

  // BACKCOMPAT: 2021.1. Use `com.intellij.openapi.actionSystem.ex.ActionUtil#getActionGroup` instead
  fun createActionGroup(vararg ids: String): ActionGroup {
    require(ids.isNotEmpty())
    val actions = ids.map { getAction(it) }
    return DefaultActionGroup(actions)
  }
}