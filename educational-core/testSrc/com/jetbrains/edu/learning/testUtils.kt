@file:Suppress("DEPRECATION")

package com.jetbrains.edu.learning

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.TestDataProvider
import com.intellij.util.ui.UIUtil
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher

inline fun <reified T> nullValue(): Matcher<T> = CoreMatchers.nullValue(T::class.java)

fun withFeature(featureId: String, enabled: Boolean, action: () -> Unit) {
  val currentValue = isFeatureEnabled(featureId)
  setFeatureEnabled(featureId, enabled)
  try {
    action()
  }
  finally {
    setFeatureEnabled(featureId, currentValue)
  }
}

inline fun withTestDialog(dialog: TestDialog, action: () -> Unit) {
  val oldDialog = TestDialogManager.setTestDialog(dialog)
  try {
    action()
  }
  finally {
    UIUtil.dispatchAllInvocationEvents()
    TestDialogManager.setTestDialog(oldDialog)
  }
}

inline fun withTestDialog(dialog: TestInputDialog, action: () -> Unit) {
  val oldDialog = TestDialogManager.setTestInputDialog(dialog)
  try {
    action()
  }
  finally {
    TestDialogManager.setTestInputDialog(oldDialog)
  }
}

fun withNotificationCheck(project: Project, disposable: Disposable, check: (Boolean, String) -> Unit, action: () -> Unit) {
  var notificationShown = false
  var notificationText = ""

  project.messageBus.connect(disposable).subscribe(Notifications.TOPIC, object : Notifications {
    override fun notify(notification: Notification) {
      notificationShown = true
      notificationText = notification.content
    }
  })

  action()
  check(notificationShown, notificationText)
}

inline fun <reified T : AnAction> getActionById(actionId: String): T {
  val action = ActionManager.getInstance().getAction(actionId) ?: error("Failed to find action by `$actionId` id")
  return action as? T ?: error("Action `$actionId` is not `${T::class.java.name}`")
}

fun testAction(
  action: AnAction,
  context: DataContext? = null,
  shouldBeEnabled: Boolean = true,
  shouldBeVisible: Boolean = shouldBeEnabled,
  runAction: Boolean = shouldBeEnabled
): Presentation {
  val e = if (context != null) TestActionEvent(context, action) else TestActionEvent(action)
  action.beforeActionPerformedUpdate(e)
  val presentation = e.presentation

  if (presentation.isEnabled != shouldBeEnabled) {
    val message = if (shouldBeEnabled) {
      "`${action.javaClass.name}` action is not enabled as expected"
    } else {
      "`${action.javaClass.name}` action is not disabled as expected"
    }
    error(message)
  }

  if (presentation.isVisible != shouldBeVisible) {
    val message = if (shouldBeVisible) {
      "`${action.javaClass.name}` action is not visible as expected"
    } else {
      "`${action.javaClass.name}` action is not invisible as expected"
    }
    error(message)
  }

  // BACKCOMPAT: 2021.1. Use the same implementation as `com.intellij.testFramework.fixtures.CodeInsightTestFixture.testAction`
  if (presentation.isEnabled && runAction) {
    action.actionPerformed(e)
  }

  return presentation
}

fun testAction(
  actionId: String,
  context: DataContext? = null,
  shouldBeEnabled: Boolean = true,
  shouldBeVisible: Boolean = shouldBeEnabled,
  runAction: Boolean = shouldBeEnabled
): Presentation {
  val action = getActionById<AnAction>(actionId)
  return testAction(action, context, shouldBeEnabled, shouldBeVisible, runAction)
}
