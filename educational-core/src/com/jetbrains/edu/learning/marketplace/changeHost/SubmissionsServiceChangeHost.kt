package com.jetbrains.edu.learning.marketplace.changeHost

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.*
import com.jetbrains.edu.learning.marketplace.MarketplaceSolutionLoader
import com.jetbrains.edu.learning.marketplace.SUBMISSIONS_SERVICE_HOST_PROPERTY
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import org.jetbrains.annotations.NonNls
import javax.swing.JComboBox
import javax.swing.JComponent

class SubmissionsServiceChangeHost : DumbAwareAction(EduCoreBundle.message("submissions.service.change.host")) {

  override fun actionPerformed(e: AnActionEvent) {
    val dialog = SubmissionsServiceChangeHostDialog()
    if (!dialog.showAndGet()) return

    val selectedHost = dialog.selectedHost
    if (selectedHost == null) {
      LOG.error("Selected Submissions service url item is null")
      return
    }

    val url = if (selectedHost in listOf(SubmissionsServiceHost.PRODUCTION, SubmissionsServiceHost.STAGING)) {
      selectedHost.url
    }
    else {
      dialog.otherServer
    }

    val existingValue = PropertiesComponent.getInstance().getValue(SUBMISSIONS_SERVICE_HOST_PROPERTY, SubmissionsServiceHost.PRODUCTION.url)
    if (existingValue == url) return

    PropertiesComponent.getInstance().setValue(SUBMISSIONS_SERVICE_HOST_PROPERTY, url, existingValue)
    LOG.info("Submissions service url was changed to $url")

    val project = e.project ?: return
    val submissionsManager = SubmissionsManager.getInstance(project)
    if (!submissionsManager.submissionsSupported()) return
    submissionsManager.prepareSubmissionsContentWhenLoggedIn { MarketplaceSolutionLoader.getInstance(project).loadSolutionsInBackground() }
  }

  private class SubmissionsServiceChangeHostDialog : DialogWrapper(true) {
    var selectedHost: SubmissionsServiceHost? = SubmissionsServiceHost.getSelectedHost()
      private set

    var otherServer: String = if (selectedHost in listOf(SubmissionsServiceHost.PRODUCTION, SubmissionsServiceHost.STAGING)) {
      SubmissionsServiceHost.OTHER.url
    }
    else {
      PropertiesComponent.getInstance().getValue(SUBMISSIONS_SERVICE_HOST_PROPERTY, SubmissionsServiceHost.OTHER.url)
    }
      private set

    private lateinit var comboBox: JComboBox<SubmissionsServiceHost>

    init {
      title = EduCoreBundle.message("submissions.service.change.host")
      setOKButtonText(EduCoreBundle.message("button.select"))
      isResizable = false
      init()
    }

    override fun createCenterPanel(): JComponent = panel {
      row(EduCoreBundle.message("submissions.service.change.host.choose.server.label")) {
        comboBox = comboBox(EnumComboBoxModel(SubmissionsServiceHost::class.java))
          .bindItem(::selectedHost)
          .focused()
          .component
      }
      row {
        textField()
          .label(EduCoreBundle.message("submissions.service.change.host.specify.url.label"))
          .bindText(::otherServer)
          .align(AlignX.FILL)
      }.visibleIf(comboBox.selectedValueMatches { it == SubmissionsServiceHost.OTHER })
    }
  }

  companion object {
    private val LOG: Logger = thisLogger()

    @NonNls
    private const val ACTION_ID = "Educational.Student.SubmissionsServiceChangeHost"
  }
}