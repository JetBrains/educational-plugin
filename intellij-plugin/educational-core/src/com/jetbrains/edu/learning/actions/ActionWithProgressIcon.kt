package com.jetbrains.edu.learning.actions

/**
 * Action that shows a [progressIcon] next to its button in the check panel while it is running.
 *
 * @see com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckPanelButtonComponent.withProgressIcon
 */
interface ActionWithProgressIcon {
  val progressIcon: ActionProgressIcon
}
