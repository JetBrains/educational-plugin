package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JComponent

class HyperskillProjectAction : DumbAwareAction("Start Hyperskill Project") {
  override fun actionPerformed(e: AnActionEvent) {
    val account = HyperskillSettings.INSTANCE.account
    if (account == null) {
      showBalloon(e, "Please, <a href=\"\">login to Hyperskill</a> and select project.", true)
    }
    else {
      val currentUser = HyperskillConnector.getCurrentUser()
      if (currentUser != null) {
        account.userInfo = currentUser
      }

      val hyperskillProject = account.userInfo.hyperskillProject
      if (hyperskillProject == null) {
        showBalloon(e, "Please, <a href=\"$HYPERSKILL_PROJECTS_URL\">select project</a> ", false)
      }
      else {
        val languageId = EduNames.JAVA
        val hyperskillCourse = HyperskillCourse(hyperskillProject.title, languageId)
        val dialog = JoinCourseDialog(hyperskillCourse)
        dialog.show()
      }
    }
  }

  private fun showBalloon(e: AnActionEvent, message: String, authorize: Boolean) {
    val builder = JBPopupFactory.getInstance()
      .createHtmlTextBalloonBuilder(message,MessageType.INFO, null)
    builder.setHideOnClickOutside(true)
    builder.setClickHandler(HSHyperlinkListener(authorize), true)
    val balloon = builder.createBalloon()

    // TODO: wrong balloon position calling action from File menu
    val component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT)
    if (component is JComponent) {
      balloon.showInCenterOf(component)
    }
  }
}

private class HSHyperlinkListener(private val authorize: Boolean) : ActionListener {
  override fun actionPerformed(e: ActionEvent?) {
    if (authorize) {
      HyperskillConnector.doAuthorize()
    }
    else {
      BrowserUtil.browse(HYPERSKILL_PROJECTS_URL)
    }
  }
}