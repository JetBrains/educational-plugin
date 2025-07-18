package com.jetbrains.edu.learning.marketplace.awsTracks.changeHost

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.services.dialog.ServiceHostChanger
import javax.swing.JComboBox
import javax.swing.JComponent

internal class AWSTracksServiceChangeHostDialog : ServiceHostChanger, DialogWrapper(true) {
  private var selectedHost: AWSTracksServiceHost? = AWSTracksServiceHost.getSelectedHost()

  private var otherServer: String = if (selectedHost in listOf(AWSTracksServiceHost.PRODUCTION, AWSTracksServiceHost.STAGING)) {
    AWSTracksServiceHost.OTHER.url
  }
  else {
    AWSTracksServiceHost.getSelectedUrl(AWSTracksServiceHost.OTHER.url)
  }

  private lateinit var comboBox: JComboBox<AWSTracksServiceHost>

  init {
    title = EduCoreBundle.message("aws.tracks.service.change.host")
    setOKButtonText(EduCoreBundle.message("button.select"))
    isResizable = true
    init()
  }

  override fun createCenterPanel(): JComponent = panel {
    row(EduCoreBundle.message("aws.tracks.service.change.host.choose.server.label")) {
      comboBox = comboBox(EnumComboBoxModel(AWSTracksServiceHost::class.java))
        .bindItem(::selectedHost)
        .focused()
        .align(Align.FILL)
        .component
    }
    row {
      textField()
        .label(EduCoreBundle.message("aws.tracks.service.change.host.specify.url.label"))
        .bindText(::otherServer)
        .align(AlignX.FILL)
    }.visibleIf(comboBox.selectedValueMatches { it == AWSTracksServiceHost.OTHER })
  }.apply {
    preferredSize = JBUI.size(WIDTH, HEIGHT)
    minimumSize = JBUI.size(WIDTH, HEIGHT)
  }

  override fun getResultUrl(): String? {
    if (!showAndGet()) return null

    val host = selectedHost ?: return null
    return if (host in listOf(AWSTracksServiceHost.PRODUCTION, AWSTracksServiceHost.STAGING)) {
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