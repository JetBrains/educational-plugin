package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.Experiments
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.intellij.ui.components.labels.ActionLink
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

class HyperskillProjectAction : DumbAwareAction("Start Hyperskill Project") {

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabledAndVisible = Experiments.isFeatureEnabled(EduExperimentalFeatures.HYPERSKILL)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val account = HyperskillSettings.INSTANCE.account
    if (account == null) {
      showBalloon(e, "Please, <a href=\"\">login to Hyperskill</a> and select project.", true)
    }
    else {
      ProgressManager.getInstance().run(object : Task.Modal(null, "Loading Selected Project", false) {
        override fun run(indicator: ProgressIndicator) {
          val currentUser = HyperskillConnector.getCurrentUser()
          if (currentUser != null) {
            account.userInfo = currentUser
          }
        }
      })

      val hyperskillProject = account.userInfo.hyperskillProject
      if (hyperskillProject == null) {
        showBalloon(e, "Please, <a href=\"$HYPERSKILL_PROJECTS_URL\">select project</a> ", false)
      }
      else if (!hyperskillProject.useIde) {
        showBalloon(e, "Selected project is not supported yet. " +
                       "Please, <a href=\"$HYPERSKILL_PROJECTS_URL\">select another project</a> ", false)
      }
      else {
        val languageId = EduNames.JAVA
        val hyperskillCourse = HyperskillCourse(hyperskillProject.title, hyperskillProject.description, languageId)
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

    val component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT)
    if (component is ActionLink) {
      balloon.showInCenterOf(component)
    }
    else {
      val relativePoint = JBPopupFactory.getInstance().guessBestPopupLocation(e.dataContext)
      balloon.show(relativePoint, Balloon.Position.above)
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