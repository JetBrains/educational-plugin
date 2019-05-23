package com.jetbrains.edu.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IconLikeCustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.ui.ClickListener
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.IconUtil
import com.intellij.util.messages.Topic
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.statistics.EduUsagesCollector
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel

abstract class LoginWidget(val project: Project, topic: Topic<EduLogInListener>) : IconLikeCustomStatusBarWidget {
  abstract val account: OAuthAccount<out Any>?
  abstract val linkName: String
  abstract val icon: Icon
  open val syncStep: SynchronizationStep? = null
  val component: JLabel = JLabel(getWidgetIcon())

  init {
    setToolTipText()
    val busConnection = project.messageBus.connect(project)
    busConnection.subscribe(topic, object : EduLogInListener {
      override fun userLoggedOut() = update()
      override fun userLoggedIn() = update()
    })
    installClickListener()
  }

  private fun installClickListener() =
    object : ClickListener() {
      override fun onClick(e: MouseEvent, clickCount: Int): Boolean {
        if (clickCount != 1) return false
        val popup = createPopup(account)
        val preferredSize = popup.content.preferredSize
        val point = Point(-preferredSize.width, -preferredSize.height)
        popup.show(RelativePoint(component, point))
        return true
      }
    }.installOn(component)

  private fun setToolTipText() {
    val logInLogOutText = if (account == null) "Log In to" else "Log Out from"
    component.toolTipText = "$logInLogOutText ${linkName}"
  }

  private fun update() {
    component.icon = getWidgetIcon()
    setToolTipText()
  }

  fun createPopup(user: OAuthAccount<out Any>?): ListPopup {
    val loginText = "Log in"
    val logOutText = "Log out"
    val syncStepName = syncStep?.stepName

    val userActionStep = if (user == null) loginText else logOutText
    val steps = mutableListOf<String>()
    if (syncStep != null && user != null && syncStep!!.syncAction.isAvailable(project)) {
      steps.add(syncStepName!!)
    }
    steps.add(userActionStep)

    val step = object : BaseListPopupStep<String>(null, steps) {
      override fun onChosen(selectedValue: String?, finalChoice: Boolean): PopupStep<*>? {
        return doFinalStep {
          when (selectedValue) {
            syncStepName -> {
              EduUsagesCollector.progressFromWidget()
              syncStep!!.syncAction.synchronizeCourse(project)
            }
            loginText -> authorize()
            logOutText -> resetAccount()
          }
        }
      }
    }
    return JBPopupFactory.getInstance().createListPopup(step)
  }

  abstract fun authorize()

  abstract fun resetAccount()

  private fun getWidgetIcon(): Icon {
    return if (account == null) {
      IconUtil.desaturate(icon) ?: error("IconUtil.desaturate failed")
    } else {
      icon
    }
  }

  override fun getComponent(): JComponent = component

  override fun getPresentation(type: StatusBarWidget.PlatformType): StatusBarWidget.WidgetPresentation? = null

  override fun install(statusBar: StatusBar) {}

  override fun dispose() {
    Disposer.dispose(this)
  }
}
