package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.progress.ProgressIndicator
import com.jetbrains.edu.learning.checkIsBackgroundThread
import com.jetbrains.edu.learning.isUnitTestMode
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

  @JvmStatic
  fun showFakeProgress(indicator: ProgressIndicator) {
    if (!isUnitTestMode) {
      checkIsBackgroundThread()
    }
    indicator.isIndeterminate = false
    indicator.fraction = 0.01
    try {
      while (indicator.isRunning) {
        Thread.sleep(1000)
        val fraction = indicator.fraction
        indicator.fraction = fraction + (1 - fraction) * 0.2
      }
    }
    catch (ignore: InterruptedException) {
      // if we remove catch block, exception will die inside pooled thread and logged, but this method can be used somewhere else
    }
  }
}