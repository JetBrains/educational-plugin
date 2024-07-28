package com.jetbrains.edu.ai.dialog

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.ai.EDU_AI_SERVICE_HOST_PROPERTY
import com.jetbrains.edu.ai.host.EduAIServiceHost
import com.jetbrains.edu.ai.host.EduAIServiceHost.*
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.JComboBox
import javax.swing.JComponent

class EduAIServiceChangeHostDialog : EduAIServiceChangeHostUI, DialogWrapper(true) {
  private var selectedHost: EduAIServiceHost? = EduAIServiceHost.getSelectedHost()

  private var otherServer: String = if (selectedHost in listOf(PRODUCTION, STAGING)) {
    OTHER.url
  }
  else {
    PropertiesComponent.getInstance().getValue(EDU_AI_SERVICE_HOST_PROPERTY, OTHER.url)
  }

  private lateinit var comboBox: JComboBox<EduAIServiceHost>

  init {
    title = EduAIBundle.message("action.Educational.EduAIServiceChangeHost.text")
    setOKButtonText(EduCoreBundle.message("button.select"))
    isResizable = true
    init()
  }

  override fun createCenterPanel(): JComponent = com.intellij.ui.dsl.builder.panel {
    row(EduAIBundle.message("ai.service.change.host.choose.server.label")) {
      comboBox = comboBox(EnumComboBoxModel(EduAIServiceHost::class.java))
        .bindItem(::selectedHost)
        .focused()
        .align(Align.FILL)
        .component
    }
    row(EduAIBundle.message("ai.service.change.host.specify.url.label")) {
      textField()
        .bindText(::otherServer)
        .align(AlignX.FILL)
    }.visibleIf(comboBox.selectedValueMatches { it == OTHER })
  }.apply {
    preferredSize = JBUI.size(WIDTH, HEIGHT)
    minimumSize = JBUI.size(WIDTH, HEIGHT)
  }

  override fun getResultUrl(): String? {
    if (!showAndGet()) return null

    val host = selectedHost ?: return null
    return if (host in listOf(PRODUCTION, STAGING)) {
      host.url
    }
    else {
      otherServer
    }
  }

  companion object {
    private const val WIDTH: Int = 350
    private const val HEIGHT: Int = 75
  }
}