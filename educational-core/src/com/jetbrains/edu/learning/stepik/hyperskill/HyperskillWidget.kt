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
    val icon = getWidgetIcon(HyperskillSettings.INSTANCE.account)
    component = JLabel(icon)

    val busConnection = ApplicationManager.getApplication().messageBus.connect()
    busConnection.subscribe(hyperskillAuthorizationTopic, object : HyperskillConnector.HyperskillLoggedIn {
      override fun userLoggedIn() {
        update()
      }
    })

    object : ClickListener() {
      override fun onClick(e: MouseEvent, clickCount: Int): Boolean {
        val account = HyperskillSettings.INSTANCE.account
        val popup = createPopup(account)
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
    val icon = getWidgetIcon(HyperskillSettings.INSTANCE.account)
    component.icon = icon
  }

  override fun getComponent(): JComponent = component

  companion object {
    const val ID = "HyperskillAccountWidget"

    private fun getWidgetIcon(user: HyperskillAccount?): Icon {
      return if (user == null) EducationalCoreIcons.HyperskillOff else EducationalCoreIcons.Hyperskill
    }

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
              }
            }
          }
        }
      }
      return JBPopupFactory.getInstance().createListPopup(stepikStep)
    }
  }
}
