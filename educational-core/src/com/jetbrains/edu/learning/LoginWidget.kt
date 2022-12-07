package com.jetbrains.edu.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.popup.ActiveIcon
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsContexts.PopupTitle
import com.intellij.openapi.util.NlsContexts.Tooltip
import com.intellij.openapi.wm.IconLikeCustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.ui.ClickListener
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.actions.EduActionUtils
import com.jetbrains.edu.learning.actions.SyncCourseAction
import com.jetbrains.edu.learning.api.EduLoginConnector
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.AuthorizationPlace
import com.jetbrains.edu.learning.ui.EduHyperlinkLabel
import java.awt.BorderLayout
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

@Suppress("UnstableApiUsage")
abstract class LoginWidget<T : OAuthAccount<out Any>>(val project: Project,
                                                      @PopupTitle private val title: String,
                                                      @Tooltip tooltipText: String,
                                                      private val icon: Icon
) : IconLikeCustomStatusBarWidget {
  abstract val connector: EduLoginConnector<T, *>

  open val synchronizeCourseActionId: String? = null

  protected abstract fun profileUrl(account: T): String

  private val component: JLabel = JBLabel(icon)

  init {
    component.toolTipText = tooltipText
    installClickListener()
  }

  private fun installClickListener() =
    object : ClickListener() {
      override fun onClick(e: MouseEvent, clickCount: Int): Boolean {
        if (clickCount != 1) return false
        val popup = createPopup()
        val preferredSize = popup.content.preferredSize
        val point = Point(-preferredSize.width, -preferredSize.height)
        popup.show(RelativePoint(component, point))
        return true
      }
    }.installOn(component)

  private fun createPopup(): JBPopup {
    val wrapperPanel = JPanel(BorderLayout())
    wrapperPanel.border = DialogWrapper.createDefaultBorder()
    val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(wrapperPanel, null)
      .setTitle(title)
      .setTitleIcon(ActiveIcon(icon, icon))
      .createPopup()

    updateContent(wrapperPanel, popup)
    return popup
  }

  private fun updateContent(wrapperPanel: JPanel, popup: JBPopup) {
    wrapperPanel.removeAll()
    wrapperPanel.add(createWidgetContent(connector.account, popup, wrapperPanel), BorderLayout.CENTER)
    wrapperPanel.revalidate()
    wrapperPanel.repaint()
    UIUtil.setBackgroundRecursively(wrapperPanel, UIUtil.getListBackground())
  }

  private fun createWidgetContent(currentAccount: T?, popup: JBPopup, wrapperPanel: JPanel): JPanel {
    val accountInfoText = if (currentAccount != null) {
      EduCoreBundle.message("account.widget.login.message", profileUrl(currentAccount), currentAccount.userInfo)
    }
    else {
      EduCoreBundle.message("account.widget.no.login.message")
    }

    val contentPanel = JBUI.Panels.simplePanel(0, 10)
    contentPanel.addToTop(EduHyperlinkLabel(accountInfoText))

    val accountActionLabel = if (currentAccount == null) {
      EduHyperlinkLabel(EduCoreBundle.message("account.widget.login"), true) {
        connector.doAuthorize(authorizationPlace = AuthorizationPlace.WIDGET)
        popup.closeOk(null)
      }
    }
    else {
      EduHyperlinkLabel(EduCoreBundle.message("account.widget.logout"), true) {
        connector.doLogout(authorizationPlace = AuthorizationPlace.WIDGET)
        updateContent(wrapperPanel, popup)
      }
    }

    val actionsPanel = JBUI.Panels.simplePanel(0, 10)
    actionsPanel.addToCenter(accountActionLabel)

    val synchronizeCourseAction = getSynchronizeCourseAction()
    if (!loginNeeded() && synchronizeCourseAction != null && synchronizeCourseAction.isAvailable(project)) {
      actionsPanel.addToBottom(EduHyperlinkLabel(synchronizeCourseAction.loginWidgetText, true) {
        synchronizeCourseAction.synchronizeCourse(project)
        popup.closeOk(null)
      })
    }

    contentPanel.addToBottom(actionsPanel)

    return contentPanel
  }

  private fun getSynchronizeCourseAction(): SyncCourseAction? {
    val synchronizeCourseActionId = synchronizeCourseActionId ?: return null
    return EduActionUtils.getAction(synchronizeCourseActionId) as? SyncCourseAction
           ?: error("Action `$synchronizeCourseActionId` should inherit `${SyncCourseAction::class.java.simpleName}`")
  }

  open fun loginNeeded(): Boolean = !connector.isLoggedIn()

  override fun getComponent(): JComponent = component

  override fun install(statusBar: StatusBar) {}

  override fun dispose() {
    Disposer.dispose(this)
  }
}
