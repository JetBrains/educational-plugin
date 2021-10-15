package com.jetbrains.edu.learning.codeforces.actions

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.impl.InternalDecoratorImpl
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.CodeforcesSettings
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionToolWindowFactory
import com.jetbrains.edu.learning.taskDescription.ui.check.CheckPanel
import com.jetbrains.edu.learning.ui.ClickableLabel
import com.jetbrains.edu.learning.ui.EduHyperlinkLabel
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Point
import javax.swing.JPanel
import javax.swing.JSeparator

class CodeforcesShowLoginStatusAction : DumbAwareAction(EduCoreBundle.lazyMessage("codeforces.account")) {

  private val POPUP_WIDTH = 280
  private val POPUP_HEIGHT = 115
  private val POPUP_OFFSET = 200

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW) ?: return
    val headerToolbarComponent = (toolWindow.component.parent as InternalDecoratorImpl).headerToolbar.component

    val popup = createPopup()
    popup.show(RelativePoint(headerToolbarComponent, Point(-POPUP_OFFSET, headerToolbarComponent.height)))
  }

  private fun createPopup(): JBPopup {
    val wrapperPanel = JPanel(BorderLayout())
    wrapperPanel.border = DialogWrapper.createDefaultBorder()
    val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(wrapperPanel, null)
      .setTitle(EduCoreBundle.message("codeforces.account"))
      .setMinSize(Dimension(POPUP_WIDTH, POPUP_HEIGHT))
      .createPopup()

    wrapperPanel.add(fillPopupContent(popup), BorderLayout.CENTER)
    return popup
  }

  private fun fillPopupContent(popup: JBPopup): JPanel {
    val account = CodeforcesSettings.getInstance().account
    val accountInfoText = if (account != null) {
      val handle = account.userInfo.handle
      EduCoreBundle.message("account.widget.login.message", "${CodeforcesNames.CODEFORCES_URL}/profile/$handle",
                            handle)
    }
    else {
      EduCoreBundle.message("account.widget.no.login.message")
    }

    val contentPanel = JBUI.Panels.simplePanel(0, 10)

    val accountActionLabel = if (account != null) {
      ClickableLabel(EduCoreBundle.message("account.widget.logout")) {
        popup.closeOk(null)
        CodeforcesSettings.getInstance().account = null
      }
    }
    else {
      ClickableLabel(EduCoreBundle.message("notification.content.authorization.action")) {
        popup.closeOk(null)
        val loginAction = ActionManager.getInstance().getAction(CodeforcesLoginAction.ACTION_ID)
        val actionEvent = AnActionEvent.createFromAnAction(loginAction,
                                                           null,
                                                           CheckPanel.ACTION_PLACE,
                                                           DataManager.getInstance().getDataContext(contentPanel))
        loginAction.actionPerformed(actionEvent)
      }
    }

    contentPanel.addToTop(EduHyperlinkLabel(accountInfoText))
    contentPanel.addToCenter(JSeparator())
    contentPanel.addToBottom(accountActionLabel)

    return contentPanel
  }

  private fun updateIcon(e: AnActionEvent) {
    e.presentation.icon = CodeforcesSettings.getInstance().getLoginIcon()
  }

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    if (!EduUtils.isStudentProject(project)) return
    if (project.course !is CodeforcesCourse) return

    presentation.isEnabledAndVisible = true
    updateIcon(e)
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Codeforces.CodeforcesShowLoginStatus"
  }
}
