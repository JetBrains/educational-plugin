package com.jetbrains.edu.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.popup.ActiveIcon
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IconLikeCustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.ui.ClickListener
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.layout.*
import com.intellij.util.IconUtil
import com.intellij.util.messages.Topic
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import java.awt.BorderLayout
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

abstract class LoginWidget(val project: Project,
                           topic: Topic<EduLogInListener>,
                           private val platformName: String
) : IconLikeCustomStatusBarWidget {
  abstract val account: OAuthAccount<out Any>?
  abstract val icon: Icon
  open val disabledIcon: Icon
    get() = IconUtil.desaturate(icon)

  open val syncStep: SynchronizationStep? = null

  private val component: JLabel = JLabel(getWidgetIcon())

  init {
    setToolTipText()
    project.messageBus.connect().subscribe(topic, object : EduLogInListener {
      override fun userLoggedOut() = update()
      override fun userLoggedIn() = update()
    })
    installClickListener()
  }

  private fun installClickListener() =
    object : ClickListener() {
      override fun onClick(e: MouseEvent, clickCount: Int): Boolean {
        if (clickCount != 1) return false
        val popup = createNewPopup(account)
        val preferredSize = popup.content.preferredSize
        val point = Point(-preferredSize.width, -preferredSize.height)
        popup.show(RelativePoint(component, point))
        return true
      }
    }.installOn(component)

  private fun setToolTipText() {
    val logInLogOutText = if (account == null) "Log In to" else "Log Out from"
    component.toolTipText = "$logInLogOutText ${platformName}"
  }

  private fun update() {
    component.icon = getWidgetIcon()
    setToolTipText()
  }

  private fun createNewPopup(account: OAuthAccount<out Any>?): JBPopup {
    val wrapperPanel = JPanel(BorderLayout())
    wrapperPanel.border = DialogWrapper.createDefaultBorder()
    val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(wrapperPanel, null)
      .setTitle("${platformName} Account")
      .setTitleIcon(ActiveIcon(icon, icon))
      .createPopup()

    val panel = panel {
      val loginText = if (account != null) {
        """Logged in as <a href="https://hyperskill.org/">${account.userInfo}</a>"""
      }
      else {
        "Not logged in to ${platformName}"
      }

      noteRow(loginText)

      if (syncStep != null && account != null && syncStep!!.syncAction.isAvailable(project)) {
        row {
          link(syncStep!!.stepName) {
            syncStep!!.syncAction.synchronizeCourse(project)
            popup.closeOk(null)
          }
        }
      }

      row {
        if (account == null) {
          link("Log in") {
            authorize()
            popup.closeOk(null)
            EduCounterUsageCollector.loggedIn(platformName, EduCounterUsageCollector.AuthorizationPlace.WIDGET)
          }
        }
        else {
          link("Log out") {
            resetAccount()
            popup.closeOk(null)
            EduCounterUsageCollector.loggedOut(platformName, EduCounterUsageCollector.AuthorizationPlace.WIDGET)
          }
        }
      }
    }
    wrapperPanel.add(panel, BorderLayout.CENTER)
    UIUtil.setBackgroundRecursively(wrapperPanel, UIUtil.getListBackground())
    return popup
  }

  abstract fun authorize()

  abstract fun resetAccount()

  private fun getWidgetIcon(): Icon {
    return if (account == null) disabledIcon else icon
  }

  override fun getComponent(): JComponent = component

  override fun install(statusBar: StatusBar) {}

  override fun dispose() {
    Disposer.dispose(this)
  }
}
