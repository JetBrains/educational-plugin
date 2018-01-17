package com.jetbrains.edu.coursecreator.configuration

import com.intellij.openapi.ui.MessageType
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel


class InvalidFormatPanel(cause: String) : JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)) {
  init {
    background = UIUtil.toAlpha(MessageType.ERROR.popupBackground, 200)
    val label = JLabel("Failed to apply configuration. $cause")
    label.border = BorderFactory.createEmptyBorder(JBUI.scale(5), JBUI.scale(10), JBUI.scale(5), 0)
    add(label)
  }
}