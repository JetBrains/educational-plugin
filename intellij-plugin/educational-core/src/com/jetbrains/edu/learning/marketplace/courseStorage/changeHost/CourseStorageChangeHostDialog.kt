package com.jetbrains.edu.learning.marketplace.courseStorage.changeHost

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

internal class CourseStorageChangeHostDialog : ServiceHostChanger, DialogWrapper(true) {
  private var selectedHost: CourseStorageServiceHost? = CourseStorageServiceHost.getSelectedHost()

  private var otherServer: String = if (selectedHost in listOf(CourseStorageServiceHost.PRODUCTION, CourseStorageServiceHost.STAGING)) {
    CourseStorageServiceHost.OTHER.url
  }
  else {
    CourseStorageServiceHost.getSelectedUrl(CourseStorageServiceHost.OTHER.url)
  }

  private lateinit var comboBox: JComboBox<CourseStorageServiceHost>

  init {
    title = EduCoreBundle.message("course.storage.change.host")
    setOKButtonText(EduCoreBundle.message("button.select"))
    isResizable = true
    init()
  }

  override fun createCenterPanel(): JComponent = panel {
    row(EduCoreBundle.message("course.storage.change.host.choose.server.label")) {
      comboBox = comboBox(EnumComboBoxModel(CourseStorageServiceHost::class.java))
        .bindItem(::selectedHost)
        .focused()
        .align(Align.FILL)
        .component
    }
    row {
      textField()
        .label(EduCoreBundle.message("course.storage.change.host.specify.url.label"))
        .bindText(::otherServer)
        .align(AlignX.FILL)
    }.visibleIf(comboBox.selectedValueMatches { it == CourseStorageServiceHost.OTHER })
  }.apply {
    preferredSize = JBUI.size(WIDTH, HEIGHT)
    minimumSize = JBUI.size(WIDTH, HEIGHT)
  }

  override fun getResultUrl(): String? {
    if (!showAndGet()) return null

    val host = selectedHost ?: return null
    return if (host in listOf(CourseStorageServiceHost.PRODUCTION, CourseStorageServiceHost.STAGING)) {
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