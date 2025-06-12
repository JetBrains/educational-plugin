package com.jetbrains.edu.ai.debugger.core.dialog

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.ai.debugger.core.EDU_AI_DEBUGGER_SERVICE_HOST_PROPERTY
import com.jetbrains.edu.ai.debugger.core.host.EduAIDebuggerServiceHost
import com.jetbrains.edu.ai.debugger.core.host.EduAIDebuggerServiceHost.OTHER
import com.jetbrains.edu.ai.debugger.core.host.EduAIDebuggerServiceHost.PRODUCTION
import com.jetbrains.edu.ai.debugger.core.host.EduAIDebuggerServiceHost.STAGING
import com.jetbrains.edu.ai.debugger.core.messages.EduAIDebuggerCoreBundle
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.services.dialog.ServiceHostChanger
import javax.swing.JComboBox
import javax.swing.JComponent

class EduAIDebuggerServiceChangeHostDialog : ServiceHostChanger, DialogWrapper(true) {
  private var selectedHost: EduAIDebuggerServiceHost? = EduAIDebuggerServiceHost.getSelectedHost()

  private var otherServer: String = PropertiesComponent.getInstance().getValue(EDU_AI_DEBUGGER_SERVICE_HOST_PROPERTY, OTHER.url)

  private lateinit var comboBox: JComboBox<EduAIDebuggerServiceHost>

  init {
    title = EduAIDebuggerCoreBundle.message("action.Educational.EduAIDebuggerServiceChangeHost.text")
    setOKButtonText(EduCoreBundle.message("button.select"))
    isResizable = true
    init()
  }

  override fun createCenterPanel(): JComponent = com.intellij.ui.dsl.builder.panel {
    row(EduAIDebuggerCoreBundle.message("ai.debugger.service.change.host.choose.server.label")) {
      comboBox = comboBox(EnumComboBoxModel(EduAIDebuggerServiceHost::class.java))
        .bindItem(::selectedHost)
        .focused()
        .align(Align.FILL)
        .component
    }
    row(EduAIDebuggerCoreBundle.message("ai.debugger.service.change.host.specify.url.label")) {
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
    return if (host in listOf(PRODUCTION, STAGING)) host.url else otherServer
  }

  companion object {
    private const val WIDTH: Int = 450
    private const val HEIGHT: Int = 75
  }
}