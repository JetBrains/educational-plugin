package com.jetbrains.edu.learning.actions.changeHost

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.JComboBox
import javax.swing.JComponent

class ChangeServiceHostDialog<E>(
  private val serviceHostManager: ServiceHostManager<E>,
  dialogTitle: @NlsContexts.DialogTitle String
) : DialogWrapper(true) where E : Enum<E>, E : ServiceHostEnum {

  private var selectedHost: E?
  private var serverUrl: String

  init {
    val (host, url) = serviceHostManager.selectedHost
    selectedHost = host
    serverUrl = if (host == serviceHostManager.other) url else serviceHostManager.other.url
    title = dialogTitle
    setOKButtonText(EduCoreBundle.message("button.select"))
    isResizable = true
    init()
  }

  override fun createCenterPanel(): JComponent {
    lateinit var comboBox: JComboBox<E>
    return panel {
      row(EduCoreBundle.message("change.service.host.label")) {
        comboBox = comboBox(EnumComboBoxModel(serviceHostManager.hostEnumClass), SimpleListCellRenderer.create("") { it.visibleName() })
          .bindItem(::selectedHost)
          .focused()
          .align(AlignX.FILL)
          .component
      }
      row(EduCoreBundle.message("change.service.host.specify.url.label")) {
        textField()
          .bindText(::serverUrl)
          .align(AlignX.FILL)
      }.visibleIf(comboBox.selectedValueMatches { it == serviceHostManager.other })
    }.apply {
      preferredSize = JBUI.size(WIDTH, HEIGHT)
      minimumSize = JBUI.size(WIDTH, HEIGHT)
    }
  }

  fun showAndGetSelectedHost(): ServiceHostManager.SelectedServiceHost<E>? {
    if (!showAndGet()) return null
    val host = selectedHost ?: return null
    return ServiceHostManager.SelectedServiceHost(host, serverUrl)
  }

  companion object {
    private const val WIDTH: Int = 350
    private const val HEIGHT: Int = 75
  }
}
