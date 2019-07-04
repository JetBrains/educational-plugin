package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.labels.LinkListener
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel

class InvalidFormatPanel(project: Project, cause: String) : JPanel(BorderLayout()) {
  init {
    val textPanel = JPanel(BorderLayout())
    val label = JLabel("Failed to apply configuration: ${cause.decapitalize()}")
    label.border = BorderFactory.createEmptyBorder(JBUI.scale(5), JBUI.scale(10), JBUI.scale(5), 0)
    textPanel.add(label, BorderLayout.CENTER)

    val linkPanel = JPanel(BorderLayout())
    val helpLink = LinkLabel("Help", null, LinkListener<Any> { _, _ ->
      showYamlTab(project)
    })
    helpLink.border = BorderFactory.createEmptyBorder(JBUI.scale(5), JBUI.scale(10), JBUI.scale(5), JBUI.scale(5))
    linkPanel.add(helpLink, BorderLayout.CENTER)

    add(textPanel, BorderLayout.CENTER)
    add(linkPanel, BorderLayout.EAST)

    UIUtil.setBackgroundRecursively(this, UIUtil.toAlpha(MessageType.ERROR.popupBackground, 200))
  }

}