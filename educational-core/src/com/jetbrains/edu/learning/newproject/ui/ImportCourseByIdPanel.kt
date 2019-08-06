package com.jetbrains.edu.learning.newproject.ui

import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.GroupLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

open class ImportCourseByIdPanel(courseLinkLabelText: String,
                                 helpLabelText: String) {
  val courseLinkLabel = JLabel(courseLinkLabelText)
  val courseURLTextField = JTextField()
  val panel = JPanel(BorderLayout())
  val helpLabel = JLabel(helpLabelText)
  open val panelSize = Dimension(400, 50)

  init {
    helpLabel.foreground = UIUtil.getLabelDisabledForeground()
    helpLabel.font = UIUtil.getLabelFont()
  }

  fun initPanel(){
    panel.add(createNestedPanel(), BorderLayout.NORTH)
    setPanelSize(panelSize)
  }

  open fun createNestedPanel(): JPanel {
    val nestedPanel = JPanel()
    val layout = GroupLayout(nestedPanel)
    layout.autoCreateGaps = true
    nestedPanel.layout = layout
    layout.setHorizontalGroup(
      layout.createSequentialGroup()
        .addComponent(courseLinkLabel)
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(courseURLTextField)
                    .addComponent(helpLabel))
    )
    layout.setVerticalGroup(
      layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(courseLinkLabel)
                    .addComponent(courseURLTextField))
        .addComponent(helpLabel)
    )
    return nestedPanel
  }

  fun setPanelSize(dimension: Dimension, isMinimumSizeEqualsPreferred: Boolean = true){
    panel.preferredSize = JBUI.size(dimension)
    if (isMinimumSizeEqualsPreferred) {
      panel.minimumSize = panel.preferredSize
    }
  }
}