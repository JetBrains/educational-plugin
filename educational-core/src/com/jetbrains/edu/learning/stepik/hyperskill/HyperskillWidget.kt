package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.application.ApplicationManager
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
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConnector.hyperskillAuthorizationTopic
import icons.EducationalCoreIcons
import java.awt.Point
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel

class HyperskillWidget : IconLikeCustomStatusBarWidget {
  private val component: JLabel

  init {
    val account = HyperskillSettings.INSTANCE.account
    val icon = getWidgetIcon(account)
    component = JLabel(icon)
    val logInLogOutText = if (account == null) "Log In to" else "Log Out from"
    component.toolTipText = "$logInLogOutText hyperskill.org"

    val busConnection = ApplicationManager.getApplication().messageBus.connect()
    busConnection.subscribe(hyperskillAuthorizationTopic, object : HyperskillConnector.HyperskillLoggedIn {
      override fun userLoggedOut() {
        update()
      }

      override fun userLoggedIn() {
        update()
      }
    })

    object : ClickListener() {
      override fun onClick(e: MouseEvent, clickCount: Int): Boolean {
        val popup = createPopup(HyperskillSettings.INSTANCE.account)
        val preferredSize = popup.content.preferredSize
        val point = Point(-preferredSize.width, -preferredSize.height)
        popup.show(RelativePoint(component, point))
        return true
      }
    }.installOn(component)
  }

  override fun ID(): String = ID

  override fun getPresentation(type: StatusBarWidget.PlatformType): StatusBarWidget.WidgetPresentation? = null

  override fun install(statusBar: StatusBar) {}

  override fun dispose() {
    Disposer.dispose(this)
  }

  private fun update() {
    val account = HyperskillSettings.INSTANCE.account
    val icon = getWidgetIcon(account)
    component.icon = icon
    val logInLogOutText = if (account == null) "Log In to" else "Log Out from"
    component.toolTipText = "$logInLogOutText hyperskill.org"
  }

  override fun getComponent(): JComponent = component

  private fun createPopup(user: HyperskillAccount?): ListPopup {
    val loginText = "Log in "
    val logOutText = "Log out"
    val userActionStep = if (user == null) loginText else logOutText
    val steps = ArrayList<String>()
    steps.add(userActionStep)

    val stepikStep = object : BaseListPopupStep<String>(null, steps) {
      override fun onChosen(selectedValue: String?, finalChoice: Boolean): PopupStep<*>? {
        return doFinalStep {
          when (selectedValue) {
            loginText -> {
              HyperskillConnector.doAuthorize()
            }
            logOutText -> {
              HyperskillSettings.INSTANCE.account = null
              val messageBus = ApplicationManager.getApplication().messageBus
              messageBus.syncPublisher<HyperskillConnector.HyperskillLoggedIn>(hyperskillAuthorizationTopic).userLoggedOut()
            }
          }
        }
      }
    }
    return JBPopupFactory.getInstance().createListPopup(stepikStep)
  }

  companion object {
    private fun getWidgetIcon(user: HyperskillAccount?): Icon {
      return if (user == null) {
        IconUtil.desaturate(EducationalCoreIcons.Hyperskill) ?: error("IconUtil.desaturate failed")
      } else {
        EducationalCoreIcons.Hyperskill
      }
    }
    const val ID = "HyperskillAccountWidget"
  }
}
