package com.jetbrains.edu.learning.marketplace.changeHost

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.marketplace.SUBMISSIONS_SERVICE_HOST_PROPERTY
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.services.dialog.ServiceHostChanger
import javax.swing.JComboBox
import javax.swing.JComponent

internal class SubmissionsServiceChangeHostDialog(val ltiLinkId: String?) : ServiceHostChanger, DialogWrapper(true) {
  private var selectedHost: SubmissionsServiceHost? = SubmissionsServiceHost.getSelectedHost()

  private var otherServer: String = if (selectedHost in listOf(SubmissionsServiceHost.PRODUCTION, SubmissionsServiceHost.STAGING)) {
    SubmissionsServiceHost.OTHER.url
  }
  else {
    PropertiesComponent.getInstance().getValue(SUBMISSIONS_SERVICE_HOST_PROPERTY, SubmissionsServiceHost.OTHER.url)
  }

  private lateinit var comboBox: JComboBox<SubmissionsServiceHost>

  init {
    title = EduCoreBundle.message("submissions.service.change.host")
    setOKButtonText(EduCoreBundle.message("button.select"))
    isResizable = true
    init()
  }

  override fun createCenterPanel(): JComponent = panel {
    row(EduCoreBundle.message("submissions.service.change.host.choose.server.label") + " " + ltiLinkId) {
      comboBox = comboBox(EnumComboBoxModel(SubmissionsServiceHost::class.java))
        .bindItem(::selectedHost)
        .focused()
        .align(Align.FILL)
        .component
    }
    row {
      textField()
        .label(EduCoreBundle.message("submissions.service.change.host.specify.url.label"))
        .bindText(::otherServer)
        .align(AlignX.FILL)
    }.visibleIf(comboBox.selectedValueMatches { it == SubmissionsServiceHost.OTHER })
  }.apply {
    preferredSize = JBUI.size(WIDTH, HEIGHT)
    minimumSize = JBUI.size(WIDTH, HEIGHT)
  }

  override fun getResultUrl(): String? {
    if (!showAndGet()) return null

    val host = selectedHost ?: return null
    return if (host in listOf(SubmissionsServiceHost.PRODUCTION, SubmissionsServiceHost.STAGING)) {
      host.url
    }
    else {
      otherServer
    }
  }

  companion object {
    private const val WIDTH: Int = 300
    private const val HEIGHT: Int = 70
  }
}